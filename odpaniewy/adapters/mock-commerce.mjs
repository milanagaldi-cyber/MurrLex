import { randomUUID } from 'node:crypto';
import { catalog } from '../catalog.mjs';
export class ShopError extends Error { constructor(message,status=400){super(message);this.status=status;} }
export class MockCommerce {
  catalog(){ return catalog; }
  createCart(){return {items:[],coupon:'',revision:0,receipts:new Map()};}
  product(id,variant){
    const product=catalog.find(p=>p.id===id), option=product?.variants.find(v=>v.id===variant);
    if(!product || !option) throw new ShopError('Wybierz prawidłowy produkt i wariant.');
    return {product,option};
  }
  cart(cart){
    const items=cart.items.map(item=>{
      const {product,option}=this.product(item.id,item.variant);
      const colors=product.group==='single'&&['turkus','fiolet','roz','pomarancz'].includes(item.variant)?[item.variant]:product.colors;
      return {...item,key:`${item.id}:${item.variant}`,name:product.name,slug:product.slug,colors,group:product.group,category:product.category,variantLabel:option.label,stock:option.stock,price:product.price,lineTotal:product.price*item.quantity};
    });
    const subtotal=items.reduce((a,i)=>a+i.lineTotal,0);
    const eligible=items.filter(i=>i.category==='szczoteczki').reduce((a,i)=>a+i.lineTotal,0);
    const discount=cart.coupon==='WIOSNA'?Math.round(eligible*5/100):0;
    const shipping=items.length?1200:0;
    return {items,count:items.reduce((a,i)=>a+i.quantity,0),subtotal,discount,shipping,total:subtotal-discount+shipping,coupon:cart.coupon,revision:cart.revision,demo:true};
  }
  update(cart,{id,variant,quantity}){
    const {option}=this.product(id,variant);
    if(!Number.isInteger(quantity)||quantity<0||quantity>option.stock) throw new ShopError(`Dostępne w demo: ${option.stock} szt.`,409);
    const index=cart.items.findIndex(i=>i.id===id&&i.variant===variant);
    if(quantity===0){if(index>=0)cart.items.splice(index,1);}
    else if(index>=0)cart.items[index].quantity=quantity;
    else cart.items.push({id,variant,quantity});
    cart.revision++;
    return this.cart(cart);
  }
  coupon(cart,{code}){
    if(typeof code!=='string'||code.length>40)throw new ShopError('Nieprawidłowy kod.');
    const value=code.trim().toUpperCase();
    if(value&&value!=='WIOSNA')throw new ShopError('Ten kod nie działa. Wypróbuj WIOSNA.');
    cart.coupon=value;cart.revision++;return this.cart(cart);
  }
  checkout(cart,data){
    if(Object.keys(data).some(k=>!['key','revision','point','scenario'].includes(k)))throw new ShopError('Demo nie przyjmuje danych osobowych.');
    const {key,revision,point,scenario}=data;
    if(typeof key!=='string'||!/^[a-zA-Z0-9-]{16,80}$/.test(key))throw new ShopError('Nieprawidłowy identyfikator próby.');
    if(cart.receipts.has(key))return cart.receipts.get(key);
    if(revision!==cart.revision)throw new ShopError('Koszyk się zmienił. Sprawdź podsumowanie.',409);
    if(!['DEMO-01','DEMO-02'].includes(point))throw new ShopError('Wybierz punkt demonstracyjny.');
    if(!['success','failure'].includes(scenario))throw new ShopError('Wybierz scenariusz testowy.');
    const snapshot=this.cart(cart);
    if(!snapshot.items.length)throw new ShopError('Koszyk jest pusty.');
    for(const i of cart.items){if(i.quantity>this.product(i.id,i.variant).option.stock)throw new ShopError('Produkt niedostępny.',409);}
    if(scenario==='failure')throw new ShopError('Symulacja odmowy. Nic nie zostało opłacone. Możesz spróbować ponownie.',422);
    const receipt={id:`DEMO-${randomUUID().slice(0,8).toUpperCase()}`,point,snapshot,paid:false,realOrder:false,demo:true};
    cart.receipts.set(key,receipt);
    while(cart.receipts.size>20)cart.receipts.delete(cart.receipts.keys().next().value);
    cart.items=[];cart.coupon='';cart.revision++;
    return receipt;
  }
}
