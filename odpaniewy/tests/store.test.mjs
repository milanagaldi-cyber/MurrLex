import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {randomUUID} from 'node:crypto';
import {mkdtemp,writeFile,rm} from 'node:fs/promises';
import {createShopServer,subscriptionDecision} from '../server.mjs';
import {MockCommerce} from '../adapters/mock-commerce.mjs';

// Node fetch normalizes Host. Raw HTTP is needed to exercise host validation.
function fetch(url,options={}){return new Promise((resolve,reject)=>{
 const req=http.request(url,{method:options.method||'GET',headers:options.headers,socketPath:options.socketPath},res=>{let body='';res.setEncoding('utf8');res.on('data',chunk=>body+=chunk);res.on('end',()=>resolve({status:res.statusCode,headers:{get:name=>{const value=res.headers[name.toLowerCase()];return Array.isArray(value)?value.join('; '):value||null;}},text:async()=>body,json:async()=>JSON.parse(body)}));});
 req.on('error',reject);req.end(options.body);
});}

const active={authenticated:true,id:'test-user-a',subscription:{status:'active',expiresAt:'2099-01-01T00:00:00Z'}};
test('deny missing integration at startup',()=>assert.throws(()=>createShopServer(),/Refusing/));
test('VPN mode requires exact HTTPS origin and cannot mix with local demo',()=>{
 for(const origin of [null,'http://shop.invalid','https://shop.invalid/','https://user@shop.invalid'])assert.throws(()=>createShopServer({accessMode:'vpn-socket',origin}));
 assert.throws(()=>createShopServer({accessMode:'vpn-socket',localDemo:true}));
 assert.throws(()=>createShopServer({accessMode:'public',origin:'https://shop.invalid'}));
});
test('subscription decision: guest, absent, active, expired and invalid',()=>{
 assert.equal(subscriptionDecision(null),401);
 assert.equal(subscriptionDecision({...active,authenticated:'false'}),401);
 assert.equal(subscriptionDecision({authenticated:true,id:'x'}),403);
 assert.equal(subscriptionDecision(active),200);
 for(const expiresAt of ['2020-01-01','invalid',null])assert.equal(subscriptionDecision({...active,subscription:{status:'active',expiresAt}}),403);
 assert.equal(subscriptionDecision({...active,subscription:{...active.subscription,status:'cancelled'}}),403);
});
test('exact totals, coupon scope, quantities and empty cart',()=>{
 const m=new MockCommerce(),c=m.createCart();
 m.update(c,{id:'DEMO-BRUSH-01',variant:'duo',quantity:1,price:1});
 let result=m.coupon(c,{code:' wiosna '});
 assert.equal(result.subtotal,5600);assert.equal(result.discount,280);assert.equal(result.total,6520);
 result=m.update(c,{id:'DEMO-BRUSH-01',variant:'duo',quantity:2});assert.equal(result.total,11840);
 result=m.update(c,{id:'DEMO-PASTE-01',variant:'demo',quantity:1});assert.equal(result.total,13640);assert.equal(result.discount,560);
 m.update(c,{id:'DEMO-BRUSH-01',variant:'duo',quantity:0});result=m.update(c,{id:'DEMO-PASTE-01',variant:'demo',quantity:0});assert.equal(result.total,0);
});
test('required variant, invalid coupon and stock limits',()=>{
 const m=new MockCommerce(),c=m.createCart();
 for(const data of [{id:'DEMO-BRUSH-01',quantity:1},{id:'fake',variant:'duo',quantity:1},{id:'DEMO-BRUSH-06',variant:'fiolet',quantity:1},{id:'DEMO-BRUSH-01',variant:'duo',quantity:9},{id:'DEMO-BRUSH-01',variant:'duo',quantity:-1},{id:'DEMO-BRUSH-01',variant:'duo',quantity:1.5}])assert.throws(()=>m.update(c,data));
 assert.throws(()=>m.coupon(c,{code:'fake'}));assert.equal(c.coupon,'');assert.equal(m.cart(c).count,0);
});
test('checkout failure, retry, idempotency, stale cart and PII rejection',()=>{
 const m=new MockCommerce(),c=m.createCart();m.update(c,{id:'DEMO-BRUSH-01',variant:'duo',quantity:1});
 const data={key:'test-checkout-key-1234',revision:c.revision,point:'DEMO-01',scenario:'success'};
 assert.throws(()=>m.checkout(c,{...data,email:'not-accepted@example.invalid'}),/osobowych/);
 assert.throws(()=>m.checkout(c,{...data,revision:0}),/zmienił/);
 assert.throws(()=>m.checkout(c,{...data,scenario:'failure'}),/odmowy/);assert.equal(m.cart(c).count,1);
 const receipt=m.checkout(c,data);assert.equal(receipt.paid,false);assert.equal(receipt.realOrder,false);assert.equal(m.cart(c).count,0);
 assert.deepEqual(m.checkout(c,data),receipt);assert.equal(receipt.snapshot.total,6800);
});
async function runServer(t,opts){const s=createShopServer(opts);await new Promise(resolve=>s.listen(0,'127.0.0.1',resolve));t.after(()=>new Promise(resolve=>{s.closeAllConnections();s.close(resolve);}));return `http://127.0.0.1:${s.address().port}`;}
test('VPN mode refuses TCP, including spoofed VPN and forwarding headers',async t=>{
 const endpoint=await runServer(t,{accessMode:'vpn-socket',origin:'https://shop.invalid',base:'/'});
 for(const path of ['/szczoteczki','/api/bootstrap','/api/checkout','/app.js','/styles.css','/assets/story.mp4']){
  const r=await fetch(endpoint+path,{headers:{Host:'shop.invalid','X-Forwarded-For':'10.0.0.21','X-ODP-VPN':'true'}});
  assert.equal(r.status,403,path);assert.match(r.headers.get('cache-control'),/no-store/);
 }
});
test('private socket allows guests with isolated carts and enforces host, CSRF and Origin',async t=>{
 const origin='https://shop.invalid';
 const socketPath=process.platform==='win32'?`\\\\.\\pipe\\odp-${randomUUID()}`:join(tmpdir(),`odp-${randomUUID()}.sock`);
 const assetRoot=await mkdtemp(join(tmpdir(),'odp-media-test-'));
 await writeFile(join(assetRoot,'hero.webp'),'test-fixture');
 t.after(()=>rm(assetRoot,{recursive:true,force:true}));
 const s=createShopServer({accessMode:'vpn-socket',origin,base:'/',assetRoot});
 await new Promise((resolve,reject)=>{s.once('error',reject);s.listen(socketPath,resolve);});
 t.after(()=>new Promise(resolve=>{s.closeAllConnections();s.close(resolve);}));
 const request=(path,opts={})=>fetch('http://shop.invalid'+path,{...opts,socketPath,headers:{Host:'shop.invalid',...opts.headers}});
 for(const path of ['/szczoteczki','/app.js','/styles.css','/assets/hero.webp'])assert.equal((await request(path)).status,200,path);
 assert.equal((await request('/szczoteczki',{headers:{Host:'evil.invalid'}})).status,400);
 const a=await request('/api/bootstrap'),b=await request('/api/bootstrap');
 const ad=await a.json(),bd=await b.json();assert.equal(ad.mode,'vpn-demo');
 const ac=a.headers.get('set-cookie').split(';')[0],bc=b.headers.get('set-cookie').split(';')[0];assert.notEqual(ac,bc);assert.notEqual(ad.csrf,bd.csrf);
 assert.match(a.headers.get('set-cookie'),/HttpOnly/);assert.match(a.headers.get('set-cookie'),/Secure/);
 const post=(path,data,headers={})=>request(path,{method:'POST',headers:{Cookie:ac,Origin:origin,'X-CSRF-Token':ad.csrf,'Content-Type':'application/json',...headers},body:JSON.stringify(data)});
 assert.equal((await post('/api/cart/item',{id:'DEMO-BRUSH-01',variant:'duo',quantity:1})).status,200);
 assert.equal((await (await request('/api/cart',{headers:{Cookie:bc}})).json()).count,0);
 assert.equal((await post('/api/cart/coupon',{code:'WIOSNA'},{Origin:'https://evil.invalid'})).status,403);
 assert.equal((await post('/api/cart/coupon',{code:'WIOSNA'},{'X-CSRF-Token':''})).status,403);
 const cart=await (await post('/api/cart/coupon',{code:'WIOSNA'})).json();assert.equal(cart.total,6520);
 const checkout=await post('/api/checkout',{key:'vpn-checkout-key-12345',revision:cart.revision,point:'DEMO-01',scenario:'success'});
 assert.equal(checkout.status,200);assert.equal((await checkout.json()).realOrder,false);
});
test('standalone root deployment keeps routes, assets, API and cart cookie independent',async t=>{
 for(const base of ['','/']){
  const endpoint=await runServer(t,{localDemo:true,base});
  const redirect=await fetch(endpoint+'/');assert.equal(redirect.status,302);assert.equal(redirect.headers.get('location'),'/szczoteczki');
  const page=await fetch(endpoint+'/szczoteczki');assert.equal(page.status,200);
  const html=await page.text();assert.ok(!html.includes('__BASE__'));assert.ok(!html.includes('/odpaniewy/'));
  for(const path of ['/produkt/curaprox-kids-duo-little-bacteria','/koszyk','/zamowienie','/app.js','/styles.css'])assert.equal((await fetch(endpoint+path)).status,200,path);
  const bootstrap=await fetch(endpoint+'/api/bootstrap');assert.equal(bootstrap.status,200);assert.match(bootstrap.headers.get('set-cookie'),/Path=\/;/);
  assert.ok((await bootstrap.json()).catalog);
 }
});
test('HTTP access matrix protects HTML, direct APIs, source and media on every request',async t=>{
 let principal=null,calls=0;const origin='https://staging.example.invalid';
 const endpoint=await runServer(t,{origin,authorize:async()=>{calls++;return principal;}});
 const paths=['/szczoteczki','/koszyk','/zamowienie','/api/bootstrap','/api/cart','/api/checkout','/assets/hero.webp','/assets/story.mp4','/app.js','/styles.css'];
 const headers={Host:'staging.example.invalid'};
 for(const [value,status] of [[null,401],[{authenticated:true,id:'x'},403],[{...active,subscription:{status:'active',expiresAt:'2020-01-01'}},403]]){
  principal=value;
  for(const path of paths){const r=await fetch(endpoint+'/odpaniewy'+path,{headers});assert.equal(r.status,status,path);assert.match(r.headers.get('cache-control'),/no-store/);assert.match(r.headers.get('vary'),/Cookie/);assert.equal(r.headers.get('cdn-cache-control'),'no-store');assert.ok(!(await r.text()).includes('Mały rytuał'));}
 }
 principal=active;const allowed=await fetch(endpoint+'/odpaniewy/szczoteczki',{headers});assert.equal(allowed.status,200);assert.match(allowed.headers.get('cache-control'),/no-store/);
 principal=null;const denied=await fetch(endpoint+'/odpaniewy/szczoteczki',{headers:{...headers,'If-None-Match':'fake'}});assert.equal(denied.status,401);assert.notEqual(denied.status,304);
 assert.equal(calls,32);
});
test('HTTP entitlement outage fails closed',async t=>{
 const endpoint=await runServer(t,{origin:'https://staging.example.invalid',authorize:async()=>{throw Error('unavailable');}});
 const r=await fetch(endpoint+'/odpaniewy/api/bootstrap',{headers:{Host:'staging.example.invalid'}});assert.equal(r.status,503);assert.match(r.headers.get('cache-control'),/no-store/);
});
test('HTTP cart isolation, CSRF, malicious price and expiry after login',async t=>{
 let principal=active;const origin='https://staging.example.invalid';const endpoint=await runServer(t,{origin,authorize:async()=>principal});
 const host={Host:'staging.example.invalid'};
 const get=async(cookie)=>{const r=await fetch(endpoint+'/odpaniewy/api/bootstrap',{headers:{...host,...(cookie?{Cookie:cookie}:{})}});return {data:await r.json(),cookie:r.headers.get('set-cookie')?.split(';')[0],r};};
 const a=await get();assert.match(a.r.headers.get('set-cookie'),/HttpOnly/);assert.match(a.r.headers.get('set-cookie'),/Secure/);assert.match(a.r.headers.get('set-cookie'),/SameSite=Strict/);
 const b=await get();
 const post=async(path,data,extra={})=>fetch(endpoint+'/odpaniewy/api'+path,{method:'POST',headers:{...host,Cookie:a.cookie,Origin:origin,'Content-Type':'application/json','X-CSRF-Token':a.data.csrf,...extra},body:JSON.stringify(data)});
 let r=await post('/cart/item',{id:'DEMO-BRUSH-01',variant:'duo',quantity:1,price:1});assert.equal(r.status,200);assert.equal((await r.json()).subtotal,5600);
 const separate=await get(b.cookie);assert.equal(separate.data.cart.count,0);
 assert.equal((await post('/cart/item',{id:'DEMO-BRUSH-01',variant:'duo',quantity:2},{'X-CSRF-Token':'wrong'})).status,403);
 assert.equal((await post('/cart/item',{id:'DEMO-BRUSH-01',variant:'duo',quantity:2},{'X-CSRF-Token':'é'.repeat(64)})).status,403);
 assert.equal((await post('/cart/coupon',{code:'WIOSNA'},{Origin:'https://evil.invalid'})).status,403);
 principal={...active,id:'test-user-b'};assert.equal((await get(a.cookie)).data.cart.count,0);
 principal={...active,subscription:{status:'active',expiresAt:'2020-01-01'}};assert.equal((await post('/cart/coupon',{code:'WIOSNA'})).status,403);
});
test('local preview refuses proxy access and alternate host; unknown paths not served',async t=>{
 const endpoint=await runServer(t,{localDemo:true});
 assert.equal((await fetch(endpoint+'/odpaniewy/szczoteczki',{headers:{'X-Forwarded-For':'127.0.0.1'}})).status,403);
 assert.equal((await fetch(endpoint+'/odpaniewy/szczoteczki',{headers:{Host:'evil.invalid'}})).status,403);
 assert.equal((await fetch(endpoint+'/odpaniewy/server.mjs')).status,404);
 assert.equal((await fetch(endpoint+'/odpaniewy/assets/../server.mjs')).status,404);
 assert.equal((await fetch(endpoint+'/odpaniewy/szczoteczki')).status,200);
});
