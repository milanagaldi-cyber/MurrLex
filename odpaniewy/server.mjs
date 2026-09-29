import http from 'node:http';
import {readFile,chmod} from 'node:fs/promises';
import {resolve,dirname,extname,sep,isAbsolute} from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
import {randomBytes,timingSafeEqual} from 'node:crypto';
import {MockCommerce,ShopError} from './adapters/mock-commerce.mjs';

const root=dirname(fileURLToPath(import.meta.url));
const loopback=ip=>['127.0.0.1','::1','::ffff:127.0.0.1'].includes(ip);
export function subscriptionDecision(principal,now=Date.now()){
  if(principal?.authenticated!==true)return 401;
  if(typeof principal.id!=='string'||!principal.id||principal.subscription?.status!=='active'||!Number.isFinite(Date.parse(principal.subscription?.expiresAt))||Date.parse(principal.subscription.expiresAt)<=now)return 403;
  return 200;
}
export function createShopServer({localDemo=false,accessMode='subscription',authorize=null,base='/odpaniewy',origin=null,assetRoot=resolve(root,'public/assets'),commerce=new MockCommerce()}={}){
  // A separate domain serves the same shop at its root; staging can use a prefix.
  if(base==='/')base='';
  if(base!==''&&!/^\/[a-z0-9-]+$/.test(base))throw new Error('Invalid base path');
  if(!['subscription','vpn-socket'].includes(accessMode))throw new Error('Unknown access mode.');
  if(localDemo&&accessMode!=='subscription')throw new Error('Local demo cannot use VPN mode.');
  if(!localDemo&&(!origin||(accessMode==='subscription'&&typeof authorize!=='function')))throw new Error('Access integration and exact HTTPS origin required. Refusing to start.');
  if(!localDemo){const parsed=new URL(origin);if(parsed.protocol!=='https:'||parsed.origin!==origin)throw new Error('Staging requires an exact HTTPS origin.');}
  const sessions=new Map();
  const server=http.createServer(async(req,res)=>{
    res.setHeader('Cache-Control','private, no-store, max-age=0');
    res.setHeader('CDN-Cache-Control','no-store');
    res.setHeader('Surrogate-Control','no-store');
    res.setHeader('Pragma','no-cache');res.setHeader('Vary','Cookie, Authorization');
    res.setHeader('X-Robots-Tag','noindex, nofollow, noarchive');
    res.setHeader('X-Content-Type-Options','nosniff');
    res.setHeader('Referrer-Policy','no-referrer');
    res.setHeader('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; media-src 'self'; connect-src 'self'; font-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
    const send=(status,data)=>{res.statusCode=status;res.setHeader('Content-Type','application/json; charset=utf-8');res.end(JSON.stringify(data));};
    try{
      // The VPN boundary is the private Nginx listener. Only its local Unix
      // socket may reach this mode; HTTP headers never grant network access.
      if(accessMode==='vpn-socket'&&(typeof server.address()!=='string'||req.socket.remoteAddress))return send(403,{error:'Dostęp tylko przez bramę Team VPN.'});
      if(localDemo&&(!loopback(req.socket.remoteAddress)||!/^127\.0\.0\.1:\d+$/.test(req.headers.host||'')||req.headers.forwarded||req.headers['x-forwarded-for']||req.headers['x-forwarded-host']||req.headers['x-forwarded-proto']))return send(403,{error:'Local preview only. Proxy access disabled.'});
      const requestOrigin=localDemo?`http://${req.headers.host}`:origin;
      const url=new URL(req.url,requestOrigin);
      if(!localDemo&&req.headers.host!==new URL(origin).host)return send(400,{error:'Invalid host.'});
      // This trusted server callback must call the existing entitlement service on EVERY request.
      // Never derive a principal from browser headers, storage, or a query parameter.
      let principal={id:localDemo?'local-preview':'vpn-browser-session'};
      if(!localDemo&&accessMode==='subscription'){
        try{principal=await authorize(req);}
        catch{return send(503,{error:'Nie można teraz sprawdzić dostępu. Spróbuj później.'});}
        const access=subscriptionDecision(principal);
        if(access!==200)return send(access,{error:access===401?'Zaloguj się w istniejącym systemie.':'Wymagana aktywna subskrypcja.'});
      }
      if(url.pathname===base||url.pathname===`${base}/`){res.writeHead(302,{Location:`${base}/szczoteczki`});return res.end();}
      if(!url.pathname.startsWith(base+'/'))return send(404,{error:'Nie znaleziono strony.'});
      const path=url.pathname.slice(base.length);
      if(path.startsWith('/api/')){
        const now=Date.now();
        for(const [key,s] of sessions)if(now-s.touched>3600000)sessions.delete(key);
        const cookieName=localDemo?'odp_preview':'__Secure-odp_preview';
        const cookies=Object.fromEntries((req.headers.cookie||'').split(';').map(x=>x.trim().split('=')));
        let sid=cookies[cookieName],session=sessions.get(sid);
        if(!session||session.owner!==principal.id){
          if(req.method!=='GET')return send(403,{error:'Odśwież stronę przed zmianą koszyka.'});
          if(sessions.size>=5000)return send(503,{error:'Spróbuj później.'});
          sid=randomBytes(32).toString('hex');session={owner:principal.id,csrf:randomBytes(32).toString('hex'),cart:commerce.createCart(),touched:now};sessions.set(sid,session);
          res.setHeader('Set-Cookie',`${cookieName}=${sid}; HttpOnly; SameSite=Strict; Path=${base}/; Max-Age=3600${localDemo?'':'; Secure'}`);
        }
        session.touched=now;
        if(req.method==='GET'&&path==='/api/bootstrap')return send(200,{catalog:commerce.catalog(),cart:commerce.cart(session.cart),csrf:session.csrf,mode:localDemo?'local-demo':accessMode==='vpn-socket'?'vpn-demo':'protected-demo'});
        if(req.method==='GET'&&path==='/api/cart')return send(200,commerce.cart(session.cart));
        if(req.method!=='POST')return send(405,{error:'Metoda niedozwolona.'});
        const token=req.headers['x-csrf-token'];
        if(req.headers.origin!==requestOrigin||typeof token!=='string'||Buffer.byteLength(token)!==Buffer.byteLength(session.csrf)||!timingSafeEqual(Buffer.from(token),Buffer.from(session.csrf)))return send(403,{error:'Nieprawidłowe żądanie. Odśwież stronę.'});
        if(!req.headers['content-type']?.startsWith('application/json'))return send(415,{error:'Wymagany JSON.'});
        let body='';for await(const chunk of req){body+=chunk;if(Buffer.byteLength(body)>4096)throw new ShopError('Żądanie zbyt duże.',413);}
        let data;try{data=JSON.parse(body);}catch{throw new ShopError('Nieprawidłowe dane.');}
        if(!data||typeof data!=='object'||Array.isArray(data))throw new ShopError('Nieprawidłowe dane.');
        if(path==='/api/cart/item')return send(200,commerce.update(session.cart,data));
        if(path==='/api/cart/coupon')return send(200,commerce.coupon(session.cart,data));
        if(path==='/api/checkout')return send(200,commerce.checkout(session.cart,data));
        return send(404,{error:'Nie znaleziono.'});
      }
      if(!['GET','HEAD'].includes(req.method))return send(405,{error:'Metoda niedozwolona.'});
      const routes=/^\/(szczoteczki|pasty|kosmetyki|koszyk|zamowienie|kontakt|dostawa|zwroty|regulamin|prywatnosc)\/?$|^\/produkt\/[a-z0-9-]+\/?$/;
      let file,allowedRoot=resolve(root,'public');
      if(routes.test(path))file=resolve(root,'public/index.html');
      else if(path==='/app.js'||path==='/styles.css')file=resolve(root,'public',path.slice(1));
      else if(/^\/assets\/[a-z0-9.-]+$/.test(path)){allowedRoot=resolve(assetRoot);file=resolve(allowedRoot,path.slice('/assets/'.length));}
      else return send(404,{error:'Nie znaleziono strony.'});
      if(!file.startsWith(allowedRoot+sep))return send(404,{error:'Nie znaleziono.'});
      let bytes;try{bytes=await readFile(file);}catch{return send(404,{error:'Nie znaleziono pliku.'});}
      const type={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.webp':'image/webp','.jpg':'image/jpeg','.svg':'image/svg+xml','.mp4':'video/mp4'}[extname(file)];
      if(extname(file)==='.html')bytes=Buffer.from(bytes.toString().replaceAll('__BASE__',base));
      res.statusCode=200;res.setHeader('Content-Type',type);res.setHeader('Content-Length',bytes.length);res.end(req.method==='HEAD'?undefined:bytes);
    }catch(error){send(error.status||500,{error:error instanceof ShopError?error.message:'Wystąpił błąd. Spróbuj ponownie.'});}
  });
  server.requestTimeout=15000;server.headersTimeout=10000;
  return server;
}
if(process.argv[1]&&resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  const localDemo=process.argv.includes('--local-demo');
  if(localDemo&&process.env.NODE_ENV==='production')throw new Error('Local demo disabled in production');
  const accessMode=process.env.ODP_ACCESS||'subscription';
  let authorize=null;
  if(!localDemo&&process.env.ODP_AUTH_MODULE){authorize=(await import(pathToFileURL(resolve(process.env.ODP_AUTH_MODULE)))).authorize;}
  const base=process.env.ODP_BASE??'/odpaniewy';
  const server=createShopServer({localDemo,accessMode,authorize,base,origin:process.env.ODP_ORIGIN,assetRoot:process.env.ODP_ASSETS});
  if(accessMode==='vpn-socket'){
    const socket=process.env.ODP_SOCKET;
    if(process.platform!=='linux'||!socket||!isAbsolute(socket)||!socket.startsWith('/run/odpaniewy/'))throw new Error('VPN mode requires a Linux socket in /run/odpaniewy/.');
    process.umask(0o117);
    server.listen(socket,async()=>{await chmod(socket,0o660);console.log('Shop ready on private Unix socket; VPN proxy required.');});
  }else{
    const port=Number(process.env.PORT||4187);
    server.listen(port,'127.0.0.1',()=>console.log(`Preview: http://127.0.0.1:${port}${base}/szczoteczki (${localDemo?'local demo, no subscription integration':'protected mock adapter'})`));
  }
}
