import fs from 'node:fs';
import path from 'node:path';

// Authorized island wall: preserve the existing Y=94 platform and wool paths.
const root = process.cwd();
const dir = path.join(root, 'tools/mcwws-build-bridge-mcp/island-wall-20261020');
fs.mkdirSync(dir, {recursive:true});
const token = fs.readFileSync(path.join(root,'plugins/MCWWS_BuildBridge/config.yml'),'utf8').match(/token:\s*(\S+)/)[1];
async function api(endpoint, body) {
  const r = await fetch('http://127.0.0.1:8765/'+endpoint, {method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...body})});
  const j = await r.json();
  if(!r.ok || j.ok===false) throw Error(JSON.stringify(j));
  return j;
}
const C={x:-346,z:2778}, plan=new Map();
const key=(x,y,z)=>`${x},${y},${z}`;
function put(dx,y,dz,block){
  if(Math.hypot(dx,dz)>40.001 || y<95 || y>106) throw Error('Outside approved envelope');
  plan.set(key(C.x+dx,y,C.z+dz),{x:C.x+dx,y,z:C.z+dz,block:'minecraft:'+block});
}
const ann=(x,z)=>Math.hypot(x,z)>=33 && Math.hypot(x,z)<=39;
const adjacent=[[1,0],[-1,0],[0,1],[0,-1]];
function edges(x,z){return {outer:adjacent.some(([a,b])=>Math.hypot(x+a,z+b)>39),inner:adjacent.some(([a,b])=>Math.hypot(x+a,z+b)<33)};}
const gates=[{name:'west',nx:-1,nz:0,offset:-1},{name:'south',nx:0,nz:1,offset:-1},{name:'southeast',nx:31.5/Math.hypot(31.5,27.5),nz:27.5/Math.hypot(31.5,27.5),offset:0}];
function local(g,x,z){return {u:x*g.nx+z*g.nz,s:-x*g.nz+z*g.nx+g.offset};}
function gateAt(x,z){return gates.find(g=>{const {u,s}=local(g,x,z);return u>30&&Math.abs(s)<=5;});}
const cavity=[];
for(let x=-39;x<=39;x++)for(let z=-39;z<=39;z++){
  if(!ann(x,z))continue;
  const {outer,inner}=edges(x,z),g=gateAt(x,z);
  const a=(Math.atan2(z,x)+Math.PI*2)%(Math.PI*2);
  for(let y=95;y<=101;y++){
    let block=null;
    if(outer||inner)block=y===95||y===100?'polished_deepslate':y===101?'stone_bricks':'deepslate_bricks';
    if(g){
      const {u,s}=local(g,x,z),t=Math.abs(s);
      // Five-block arch with stone shoulders; side doorways link the hollow gallery.
      if(t<=4.6)block='deepslate_bricks';
      const half=y<=98?2.6:y===99?1.65:y===100?0.65:0;
      if(y<=100&&t<=half)block=null;
      if(t>2.6&&t<=4.6)block=(y===95||y===100||y===101)?'polished_deepslate':'stone_bricks';
      if(y<=97&&u>=35&&u<=37.5)block=null;
      if(y===101&&t<=2.6)block='polished_deepslate';
    }
    if(block)put(x,y,z,block);else cavity.push({x:C.x+x,y,z:C.z+z});
  }
  put(x,102,z,(outer||inner)?'polished_deepslate':'stone_bricks');
  if(outer||inner){
    put(x,103,z,'deepslate_bricks');
    if(Math.floor(a*36/2.8)%2===0)put(x,104,z,'stone_bricks');
  }
  if(g){
    const {s}=local(g,x,z);
    if(Math.abs(s)>2.6 && Math.abs(s)<=4.6 && (outer||inner)){
      put(x,104,z,'polished_deepslate');
      put(x,105,z,'deepslate_bricks');
      if(Math.abs(s)>3.5)put(x,106,z,'stone_bricks');
    }
  }
}
// Courtyard-side staircase: the roof circuit and hollow gallery stay continuous.
for(let i=0;i<16;i++){
  const z=-9+i,y=95+Math.floor(i/2);
  for(let x=29;x<=31;x++){
    for(let yy=95;yy<y;yy++)put(x,yy,z,'deepslate_bricks');
    put(x,y,z,i%2===0?'stone_brick_slab[type=bottom,waterlogged=false]':'stone_bricks');
  }
  for(let x of [28,32]){
    for(let yy=95;yy<=y;yy++)put(x,yy,z,'polished_deepslate');
    put(x,y+1,z,'polished_deepslate_wall');
  }
}
for(let x=28;x<=33;x++)for(let z=7;z<=9;z++){
  put(x,102,z,'stone_bricks');
  if(x===28||z===9)put(x,103,z,'polished_deepslate_wall');
}
for(let x of [28,31])for(let y=95;y<=101;y++)put(x,y,9,'polished_deepslate');
for(let x=32;x<=33;x++)for(let z=7;z<=8;z++)for(let y=103;y<=104;y++)plan.delete(key(C.x+x,y,C.z+z));
// The courtyard also has a ground-level doorway into the lower gallery.
for(let x=28;x<=34;x++)for(let z=-13;z<=-11;z++)for(let y=95;y<=98;y++)plan.delete(key(C.x+x,y,C.z+z));
// Ceiling-mounted gallery lighting at roughly seven-block intervals.
for(let i=0;i<32;i++){
  let a=i*Math.PI*2/32,x=Math.round(36*Math.cos(a)),z=Math.round(36*Math.sin(a));
  if(gateAt(x,z)||(x>=34&&z>=-14&&z<=8))continue;
  if(plan.has(key(C.x+x,102,C.z+z))&&!plan.has(key(C.x+x,101,C.z+z)))put(x,101,z,'lantern[hanging=true,waterlogged=false]');
}
// Lamps stand on merlons, never floating over passages.
for(let i=0;i<16;i++){
  const a=(i+.5)*Math.PI*2/16,x=Math.round(38.6*Math.cos(a)),z=Math.round(38.6*Math.sin(a));
  if(!ann(x,z)||gateAt(x,z))continue;
  if(plan.has(key(C.x+x,104,C.z+z)))put(x,105,z,'lantern[hanging=false,waterlogged=false]');
}
for(const p of plan.values())if(p.block==='minecraft:polished_deepslate_wall'){
 const sides=[['north',0,-1],['south',0,1],['east',1,0],['west',-1,0]].map(([n,dx,dz])=>`${n}=${plan.has(key(p.x+dx,p.y,p.z+dz))?'low':'none'}`);
 p.block=`minecraft:polished_deepslate_wall[${sides.join(',')},up=true,waterlogged=false]`;
}
const blocks=[...plan.values()];
fs.writeFileSync(path.join(dir,'plan.json'),JSON.stringify({center:C,gates,blocks},null,2));
console.log('Plan:',blocks.length,'blocks; envelope',JSON.stringify({x:[-385,-307],y:[95,106],z:[2739,2817]}));
if(!process.argv.includes('--apply')&&!process.argv.includes('--preflight'))process.exit(0);

async function readMany(points){
  const out=new Array(points.length);let next=0,done=0;
  await Promise.all(Array.from({length:24},async()=>{
    while(next<points.length){const i=next++;const p=points[i];out[i]={...p,block:(await api('get_block',p)).block};done++;if(done%1000===0)console.log('Read',done,'/',points.length);}
  }));return out;
}
const before=await readMany(blocks.map(({x,y,z})=>({x,y,z})));
const conflicts=before.filter(p=>!['minecraft:air','minecraft:cave_air','minecraft:void_air'].includes(p.block));
fs.writeFileSync(path.join(dir,'before.json'),JSON.stringify(before));
if(conflicts.length){fs.writeFileSync(path.join(dir,'conflicts.json'),JSON.stringify(conflicts,null,2));throw Error('Existing blocks in build volume: '+conflicts.length+'; inspect conflicts.json');}
// Sample every gallery column at standing height, ensuring the five-air gallery already is clear.
const floorSamples=cavity.filter(p=>p.y===95&&!plan.has(key(p.x,p.y,p.z)));
const cavityRead=await readMany(floorSamples);
const blocked=cavityRead.filter(p=>!['minecraft:air','minecraft:cave_air','minecraft:void_air'].includes(p.block));
if(blocked.length)throw Error('Gallery floor obstructed '+JSON.stringify(blocked.slice(0,10)));
console.log('Preflight clear; durable before.json saved.');
if(!process.argv.includes('--apply'))process.exit(0);
for(let i=0;i<blocks.length;i+=4000){
  const result=await api('set_blocks',{blocks:blocks.slice(i,i+4000)});
  fs.appendFileSync(path.join(dir,'writes.jsonl'),JSON.stringify({time:new Date().toISOString(),offset:i,result})+'\n');
  console.log('Placed',Math.min(i+4000,blocks.length),'/',blocks.length,JSON.stringify(result));
}
console.log('COMPLETE');
