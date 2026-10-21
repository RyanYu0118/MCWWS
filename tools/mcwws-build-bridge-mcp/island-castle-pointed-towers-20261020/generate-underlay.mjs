import fs from 'node:fs';
import path from 'node:path';

// User-approved envelope: X -386..-306, Z 2738..2818, Y 95..128.
// Existing platform at Y=94 and wool paths are outside this edit.
const dir='tools/mcwws-build-bridge-mcp/island-castle-pointed-towers-20261020/underlay/';
fs.mkdirSync(dir,{recursive:true});
const C={x:-346,z:2778}, M=new Map(), k=(x,y,z)=>`${x},${y},${z}`;
const old=JSON.parse(fs.readFileSync('tools/mcwws-build-bridge-mcp/island-wall-20261020/plan.json')).blocks;
const oldMap=new Map(old.map(p=>[k(p.x,p.y,p.z),p]));
function put(x,y,z,b){
 if(Math.hypot(x,z)>40.001||y<95||y>128)throw Error('Outside approved envelope '+[x,y,z]);
 M.set(k(x+C.x,y,z+C.z),{x:x+C.x,y,z:z+C.z,block:b.startsWith('minecraft:')?b:'minecraft:'+b});
}
function del(x,y,z){M.delete(k(x+C.x,y,z+C.z));}
const ann=(x,z)=>Math.hypot(x,z)>=33&&Math.hypot(x,z)<=39;
const D=[[1,0],[-1,0],[0,1],[0,-1]];
const edge=(x,z)=>({outer:D.some(([a,b])=>Math.hypot(x+a,z+b)>39),inner:D.some(([a,b])=>Math.hypot(x+a,z+b)<33)});
const gates=[{nx:-1,nz:0,offset:-1},{nx:0,nz:1,offset:-1},{nx:31.5/Math.hypot(31.5,27.5),nz:27.5/Math.hypot(31.5,27.5),offset:0}];
const local=(g,x,z)=>({u:x*g.nx+z*g.nz,s:-x*g.nz+z*g.nx+g.offset});
const gateAt=(x,z)=>gates.find(g=>{let {u,s}=local(g,x,z);return u>30&&Math.abs(s)<=5;});

// Existing ground gallery and original gate arches.
for(let x=-39;x<=39;x++)for(let z=-39;z<=39;z++){
 if(!ann(x,z))continue;
 const e=edge(x,z),g=gateAt(x,z),a=(Math.atan2(z,x)+2*Math.PI)%(2*Math.PI);
 for(let y=95;y<=101;y++){
  let b=(e.outer||e.inner)?(y===95||y===100?'polished_deepslate':y===101?'stone_bricks':'deepslate_bricks'):null;
  if(g){
   const {u,s}=local(g,x,z),t=Math.abs(s);
   if(t<=4.6)b='deepslate_bricks';
   const half=y<=98?2.6:y===99?1.65:y===100?.65:0;
   if(y<=100&&t<=half)b=null;
   if(t>2.6&&t<=4.6)b=[95,100,101].includes(y)?'polished_deepslate':'stone_bricks';
   if(y<=97&&u>=35&&u<=37.5)b=null;
   if(y===101&&t<=2.6)b='polished_deepslate';
  }
  if(b)put(x,y,z,b);
 }
 // Three continuous ring decks. Upper galleries have stone piers and open arches.
 for(const floor of [102,110,118])put(x,floor,z,(e.outer||e.inner)?'polished_deepslate':'stone_bricks');
 if(e.outer||e.inner){
  const phase=((a/(2*Math.PI)*32+.5)%1-.5)*7.07;
  for(const base of [103,111])for(let h=0;h<7;h++){
   const opening=h<=3?2.3:h===4?1.4:h===5?.65:0;
   if(Math.abs(phase)<opening){
    if(h===0)put(x,base,z,'iron_bars');
   }else put(x,base+h,z,h===6?'polished_deepslate':(Math.abs(phase)>2.5?'stone_bricks':'deepslate_tiles'));
  }
  put(x,119,z,'deepslate_bricks');
  if(Math.floor(a*36/2.8)%2===0)put(x,120,z,'stone_bricks');
 }
}
// Projecting bands and brackets articulate each storey without enlarging the disk.
for(let x=-40;x<=40;x++)for(let z=-40;z<=40;z++){
 const r=Math.hypot(x,z);if(r<=39||r>40)continue;
 for(const y of [102,110,118]){
  // Only a one-block cantilever directly against the supporting wall.
  if(D.some(([a,b])=>ann(x+a,z+b)&&edge(x+a,z+b).outer))put(x,y,z,'polished_deepslate_slab[type=top,waterlogged=false]');
 }
}
function rotate(x,z,r){for(let i=0;i<r;i++)[x,z]=[-z,x];return [x,z];}
const stairSteps=[],landings=[];
for(let rot=0;rot<4;rot++){
 const p=(x,y,z,b)=>{const [a,c]=rotate(x,z,rot);put(a,y,c,b);};
 const erase=(x,y,z)=>{const[a,c]=rotate(x,z,rot);del(a,y,c);};
 // Six pillars carry the landings and the alternating upper flights.
 for(const [x,z]of [[23,-12],[23,9],[28,-12],[28,9],[32,-12],[32,9]])for(let y=95;y<=(z<0?110:118);y++)p(x,y,z,y%8===6?'polished_deepslate':'stone_bricks');
 for(const [floor,z1,z2]of [[102,7,9],[110,-12,-10],[118,7,9]]){
  for(let x=23;x<=33;x++)for(let z=z1;z<=z2;z++){
   p(x,floor,z,'stone_bricks');
   if(x===23||z===(floor===110?z1:z2))p(x,floor+1,z,'polished_deepslate_wall');
  }
  const [x,z]=rotate(30,floor===110?-11:8,rot);landings.push({x:x+C.x,y:floor+1,z:z+C.z});
 }
 // Bases below the upper flights are supported by perimeter columns and landings.
 for(let x=23;x<=27;x++)for(let z=-9;z<=6;z++)p(x,102,z,'stone_bricks');
 for(let x=28;x<=32;x++)for(let z=-9;z<=6;z++)p(x,110,z,'stone_bricks');
 for(let flight=0;flight<3;flight++){
  const base=95+8*flight,x0=flight===1?24:29;
  for(let i=0;i<16;i++){
   const z=flight===1?6-i:-9+i,y=base+Math.floor(i/2);
   for(let x=x0;x<x0+3;x++){
    for(let yy=base;yy<y;yy++)p(x,yy,z,'deepslate_bricks');
    p(x,y,z,i%2===0?'stone_brick_slab[type=bottom,waterlogged=false]':'stone_bricks');
    const[a,c]=rotate(x,z,rot);stairSteps.push({x:a+C.x,y,z:c+C.z,half:i%2===0});
   }
   for(const x of [x0-1,x0+3]){
    for(let yy=base;yy<=y;yy++)p(x,yy,z,'polished_deepslate');
    p(x,y+1,z,'polished_deepslate_wall');
   }
  }
 }
 // Doors at each landing lead straight into the ring gallery.
 for(const [floor,z1,z2]of [[102,7,8],[110,-12,-10],[118,7,8]])for(let x=29;x<=34;x++)for(let z=z1;z<=z2;z++)for(let y=floor+1;y<=Math.min(floor+4,121);y++)erase(x,y,z);
 // Ground-level access between the courtyard and hollow wall.
 for(let x=29;x<=34;x++)for(let z=-13;z<=-11;z++)for(let y=95;y<=98;y++)erase(x,y,z);
}

// Keep the west/south entrance axes open beneath the new stair structures.
// These arches remove only modeled staircase infill, never the treads above Y=98.
for(const g of gates.slice(0,2))for(let x=-34;x<=34;x++)for(let z=-34;z<=34;z++){
 const {u,s}=local(g,x,z);
 if(u>=22&&u<=34&&Math.abs(s)<=2.6)for(let y=95;y<=98;y++)del(x,y,z);
}
const towers=[];
const towerLadders=[];
for(const t of towers){
 const norm=Math.hypot(t.x,t.z),nx=t.x/norm,nz=t.z/norm;
 const disk=(x,z,r=4)=>Math.hypot(x,z)<=r;
 for(let dx=-4;dx<=4;dx++)for(let dz=-4;dz<=4;dz++){
  if(!disk(dx,dz))continue;
  const x=t.x+dx,z=t.z+dz,boundary=D.some(([a,b])=>!disk(dx+a,dz+b));
  // Clear only the modeled overlap, then rebuild a hollow tower with tangent doors.
  for(let y=95;y<=128;y++)del(x,y,z);
  for(let y=95;y<t.deck;y++){
   if(!boundary)continue;
   const v=dx*nx+dz*nz,s=-dx*nz+dz*nx;
   const doorway=[95,103,111,119].some(b=>y>=b&&y<=b+3)&&Math.abs(v)<=1.8&&Math.abs(s)>=2;
   const slit=([105,106,113,114,121,122].includes(y))&&((dx===0&&Math.abs(dz)===4)||(dz===0&&dx===-4));
   if(!doorway&&!slit)put(x,y,z,[101,109,117,t.deck-1].includes(y)?'polished_deepslate':'stone_bricks');
   else if(slit)put(x,y,z,'iron_bars');
  }
  for(const floor of [102,110,118,t.deck])put(x,floor,z,'stone_bricks');
 }
 // Slightly projecting cap, a continuous parapet, alternating merlons.
 for(let dx=-4;dx<=4;dx++)for(let dz=-4;dz<=4;dz++){
  if(!disk(dx,dz,4.5))continue;
  const x=t.x+dx,z=t.z+dz;
  put(x,t.deck,z,'polished_deepslate');
  if(D.some(([a,b])=>!disk(dx+a,dz+b,4.5))){
   put(x,t.deck+1,z,'deepslate_bricks');
   if((dx+dz)%2===0)put(x,t.deck+2,z,'stone_bricks');
  }
 }
 // Ladder and hatch connect the wall-top to the tower lookout.
 for(let y=119;y<=t.deck;y++){
  put(t.x+4,y,t.z,'stone_bricks');
  put(t.x+3,y,t.z,'ladder[facing=west,waterlogged=false]');
  towerLadders.push({x:C.x+t.x+3,y,z:C.z+t.z});
 }
 for(const y of [101,109,117,t.deck-1])put(t.x,y,t.z,'lantern[hanging=true,waterlogged=false]');
 // Lamps sit on the cap parapet and stay below the approved height.
 for(const [dx,dz]of [[0,4],[0,-4]]){
  put(t.x+dx,t.deck+1,t.z+dz,'polished_deepslate');
  put(t.x+dx,t.deck+2,t.z+dz,'lantern[hanging=false,waterlogged=false]');
 }
}
// Gallery lamps: each is attached to the floor above it; skip tower overlaps.
for(let i=0;i<32;i++){
 const a=i*Math.PI*2/32,x=Math.round(36*Math.cos(a)),z=Math.round(36*Math.sin(a));
 if(towers.some(t=>Math.hypot(x-t.x,z-t.z)<=4.5))continue;
 for(const y of [101,109,117])if(!M.has(k(C.x+x,y,C.z+z))&&M.has(k(C.x+x,y+1,C.z+z)))put(x,y,z,'lantern[hanging=true,waterlogged=false]');
}
// Explicit connection states are necessary for BuildBridge's physics-free writes.
for(const p of M.values()){
 if(p.block!=='minecraft:polished_deepslate_wall'&&p.block!=='minecraft:iron_bars')continue;
 const wall=p.block.endsWith('_wall');
 const sides=[['north',0,-1],['south',0,1],['east',1,0],['west',-1,0]].map(([n,dx,dz])=>`${n}=${M.has(k(p.x+dx,p.y,p.z+dz))?(wall?'low':'true'):(wall?'none':'false')}`);
 p.block+=`[${sides.join(',')},${wall?'up=true,':''}waterlogged=false]`;
}
const blocks=[...M.values()],patch=[];
for(const [key,p]of oldMap)if(!M.has(key))patch.push({...p,block:'minecraft:air'});
for(const [key,p]of M)if(oldMap.get(key)?.block!==p.block)patch.push(p);
const plan={center:C,floors:[102,110,118],towers,stairSteps,landings,towerLadders,blocks,patch};
fs.writeFileSync(dir+'plan.json',JSON.stringify(plan));
console.log('PLAN',JSON.stringify({blocks:blocks.length,changes:patch.length,towers:towers.length,stairs:4,floors:plan.floors}));
if(!process.argv.includes('--apply'))process.exit(0);

const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(endpoint,body){
 const r=await fetch('http://127.0.0.1:8765/'+endpoint,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...body})});
 const j=await r.json();if(!r.ok||j.ok===false)throw Error(JSON.stringify(j));return j;
}
let next=0,done=0;const before=new Array(patch.length);
await Promise.all(Array.from({length:24},async()=>{
 while(next<patch.length){const i=next++,p=patch[i];before[i]={x:p.x,y:p.y,z:p.z,block:(await api('get_block',{x:p.x,y:p.y,z:p.z})).block};if(++done%2000===0)console.log('Snapshot',done,'/',patch.length);}
}));
const same=(a,b)=>a.split('[')[0]===b.split('[')[0];
const conflicts=before.filter(p=>!same(p.block,oldMap.get(k(p.x,p.y,p.z))?.block??'minecraft:air'));
fs.writeFileSync(dir+'before-'+Date.now()+'.json',JSON.stringify(before));
if(conflicts.length){fs.writeFileSync(dir+'conflicts.json',JSON.stringify(conflicts,null,2));throw Error('Concurrent or unmodeled blocks: '+conflicts.length);}
console.log('Preflight passed; original blocks backed up.');
// Only remove known previous construction. All additions remain in the approved envelope.
patch.sort((a,b)=>(a.block==='minecraft:air'?-1:0)-(b.block==='minecraft:air'?-1:0)||a.y-b.y);
for(let i=0;i<patch.length;i+=3000){
 const slice=patch.slice(i,i+3000),result=await api('set_blocks',{blocks:slice});
 fs.appendFileSync(dir+'writes.jsonl',JSON.stringify({offset:i,count:slice.length,time:new Date().toISOString(),result})+'\n');
 console.log('Placed',Math.min(i+3000,patch.length),'/',patch.length);
}
console.log('COMPLETE');
