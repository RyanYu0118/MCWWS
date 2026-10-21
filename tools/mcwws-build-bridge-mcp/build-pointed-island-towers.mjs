import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/island-castle-pointed-towers-20261020/';
fs.mkdirSync(dir,{recursive:true});
const source=JSON.parse(fs.readFileSync('tools/mcwws-build-bridge-mcp/island-castle-refinement-20261020/plan.json'));
const underlay=JSON.parse(fs.readFileSync(dir+'underlay-refined/plan.json'));
const C={x:-346,z:2778},key=(x,y,z)=>`${x},${y},${z}`;
const old=new Map(source.blocks.map(p=>[key(p.x,p.y,p.z),p]));
const U=new Map(underlay.blocks.map(p=>[key(p.x,p.y,p.z),p]));
const M=new Map(source.blocks.map(p=>[key(p.x,p.y,p.z),{...p}]));
const D=[[1,0],[-1,0],[0,1],[0,-1]],towerCubes=new Set();
function put(x,y,z,b,tower=false){
 const wx=x+C.x,wz=z+C.z;
 if(wx< -388||wx> -304||wz<2736||wz>2820||y<94||y>144)throw Error('Outside approved envelope '+[wx,y,wz]);
 const k=key(wx,y,wz);M.set(k,{x:wx,y,z:wz,block:b.startsWith('minecraft:')?b:'minecraft:'+b});
 if(tower)towerCubes.add(k);
}
function remove(x,y,z){M.delete(key(x+C.x,y,z+C.z));}
// Restore the finished, enclosed ring behind the old towers, including stairs/doorways.
for(const t of source.towers)for(let dx=-4;dx<=4;dx++)for(let dz=-4;dz<=4;dz++){
 if(Math.hypot(dx,dz)>4.5)continue;
 for(let y=95;y<=128;y++){
  const k=key(C.x+t.x+dx,y,C.z+t.z+dz),base=U.get(k);
  if(base)M.set(k,{...base});else M.delete(k);
 }
}
const towers=[
 {x:36,z:13,deck:126,roofBase:133,peak:144,radius:5},
 {x:-13,z:36,deck:124,roofBase:131,peak:142,radius:5},
 {x:-36,z:-13,deck:126,roofBase:133,peak:144,radius:5},
 {x:13,z:-36,deck:124,roofBase:131,peak:142,radius:5},
];
const towerLadders=[],foundationChecks=[];
for(const t of towers){
 const norm=Math.hypot(t.x,t.z),nx=t.x/norm,nz=t.z/norm;
 const disk=(dx,dz,r=5)=>Math.hypot(dx,dz)<=r;
 // Hollow body, eleven blocks across. Only known construction is removed by the diff.
 for(let dx=-5;dx<=5;dx++)for(let dz=-5;dz<=5;dz++){
  if(!disk(dx,dz))continue;
  const x=t.x+dx,z=t.z+dz,boundary=D.some(([a,b])=>!disk(dx+a,dz+b));
  foundationChecks.push({x:C.x+x,y:94,z:C.z+z});
  if(Math.hypot(x,z)>40)put(x,94,z,'polished_deepslate',true);
  for(let y=95;y<=144;y++)remove(x,y,z);
  for(let y=95;y<t.roofBase;y++){
   if(!boundary)continue;
   const v=dx*nx+dz*nz,s=-dx*nz+dz*nx;
   // Doors are on the inward portions of both tangent faces, directly on ring decks.
   const door=[95,103,111,119].some(b=>y>=b&&y<=b+3)&&v>=-3.8&&v<=0&&Math.abs(s)>=2;
   const arrow=([105,106,113,114,121,122].includes(y))&&((dx===0&&Math.abs(dz)===5)||(dz===0&&dx===-5));
   const lookout=y>=t.deck+2&&y<=t.deck+4&&((Math.abs(dx)>=4&&Math.abs(dz)<=2)||(Math.abs(dz)>=4&&Math.abs(dx)<=2));
   if(!door)put(x,y,z,arrow||lookout?'gray_stained_glass':([95,101,109,117,t.roofBase-1].includes(y)?'polished_deepslate':'deepslate_bricks'),!(arrow||lookout));
  }
  for(const y of [102,110,118,t.deck])put(x,y,z,'polished_deepslate',true);
 }
 // Thin projecting belts beneath each floor and the eaves.
 for(let dx=-5;dx<=5;dx++)for(let dz=-5;dz<=5;dz++){
  const r=Math.hypot(dx,dz);if(r<=5||r>5.5)continue;
  if(!D.some(([a,b])=>disk(dx+a,dz+b)))continue;
  for(const y of [101,109,117,t.roofBase-1])put(t.x+dx,y,t.z+dz,'polished_deepslate_slab[type=top,waterlogged=false]');
 }
 // The former open deck is now an enclosed lookout with a supported internal ladder.
 for(let y=119;y<=t.deck;y++){
  put(t.x+5,y,t.z,'polished_deepslate',true);
  put(t.x+4,y,t.z,'ladder[facing=west,waterlogged=false]');
  towerLadders.push({x:C.x+t.x+4,y,z:C.z+t.z});
 }
 // Ceiling, followed by a hollow conical tile roof. Half steps soften the silhouette.
 for(let dx=-6;dx<=6;dx++)for(let dz=-6;dz<=6;dz++)if(disk(dx,dz,6))put(t.x+dx,t.roofBase,t.z+dz,'deepslate_tiles');
 for(let h=0;h<10;h++){
  const r=6-.6*h,y=t.roofBase+h,next=r-.6;
  for(let dx=-6;dx<=6;dx++)for(let dz=-6;dz<=6;dz++){
   const d=Math.hypot(dx,dz);if(d>r)continue;
   if(h===0||d>=Math.max(0,r-1.5)||h===9)put(t.x+dx,y,t.z+dz,'deepslate_tiles');
   if(h<9&&d>next)put(t.x+dx,y+1,t.z+dz,'deepslate_tile_slab[type=bottom,waterlogged=false]');
  }
 }
 put(t.x,t.peak-1,t.z,'polished_deepslate_wall');
 put(t.x,t.peak,t.z,'iron_bars');
 for(const ceiling of [101,109,117,t.roofBase-1])put(t.x,ceiling,t.z,'lantern[hanging=true,waterlogged=false]');
}
// Match the ground's 63:18:19 material family ratio in the new masonry.
const masonry=[...towerCubes].map(k=>M.get(k)).filter(p=>p&&['minecraft:polished_deepslate','minecraft:deepslate_bricks'].includes(p.block));
const hash=p=>{let h=(Math.imul(p.x,73856093)^Math.imul(p.y,19349663)^Math.imul(p.z,83492791))>>>0;h=Math.imul(h^(h>>>16),0x7feb352d)>>>0;return(h^(h>>>15))>>>0;};
// Bands and floors retain their polished finish; wall infill supplies the brick variation.
const fixed=masonry.filter(p=>p.block==='minecraft:polished_deepslate'),body=masonry.filter(p=>p.block!=='minecraft:polished_deepslate');
body.sort((a,b)=>hash(a)-hash(b)||a.x-b.x||a.y-b.y||a.z-b.z);
const polishExtra=Math.max(0,Math.round(masonry.length*.632)-fixed.length),brickCount=Math.round(masonry.length*.18);
for(let i=0;i<body.length;i++)body[i].block=i<polishExtra?'minecraft:polished_deepslate':i<polishExtra+brickCount?'minecraft:deepslate_bricks':'minecraft:cracked_deepslate_bricks';
for(const p of M.values()){
 const id=p.block.split('[')[0],wall=id==='minecraft:polished_deepslate_wall',bar=id==='minecraft:iron_bars';
 if(!wall&&!bar)continue;
 const sides=[['north',0,-1],['south',0,1],['east',1,0],['west',-1,0]].map(([n,a,b])=>`${n}=${M.has(key(p.x+a,p.y,p.z+b))?(wall?'low':'true'):(wall?'none':'false')}`);
 p.block=`${id}[${sides.join(',')},${wall?'up=true,':''}waterlogged=false]`;
}
const blocks=[...M.values()],patch=[];
for(const[k,p]of old)if(!M.has(k))patch.push({...p,block:'minecraft:air'});
for(const[k,p]of M)if(old.get(k)?.block!==p.block)patch.push(p);
const plan={...source,blocks,patch,towers,towerLadders,foundationChecks,envelope:{x:[-388,-304],y:[94,144],z:[2736,2820]}};
fs.writeFileSync(dir+'plan.json',JSON.stringify(plan));
console.log('PLAN',JSON.stringify({blocks:blocks.length,changes:patch.length,towers,foundationChecks:foundationChecks.length}));
if(!process.argv.includes('--apply'))process.exit(0);

const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(e,body){const r=await fetch('http://127.0.0.1:8765/'+e,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...body})});const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));return j;}
async function read(list){let next=0,done=0;const out=new Array(list.length);await Promise.all(Array.from({length:24},async()=>{while(next<list.length){const i=next++,p=list[i];out[i]={x:p.x,y:p.y,z:p.z,block:(await api('get_block',{x:p.x,y:p.y,z:p.z})).block};if(++done%2500===0)console.log('Snapshot',done,'/',list.length);}}));return out;}
const cached=process.argv.includes('--cached-snapshot');
const snapshots=fs.readdirSync(dir).filter(n=>/^before-\d+\.json$/.test(n)).sort();
const before=cached?JSON.parse(fs.readFileSync(dir+snapshots.at(-1))):await read(patch);
const ground=cached?JSON.parse(fs.readFileSync(dir+'foundation-before.json')):await read(foundationChecks);
fs.writeFileSync(dir+'before-'+Date.now()+'.json',JSON.stringify(before));
fs.writeFileSync(dir+'foundation-before.json',JSON.stringify(ground));
const terrain=new Set(['minecraft:stone','minecraft:calcite','minecraft:andesite','minecraft:granite','minecraft:diorite','minecraft:dirt','minecraft:coarse_dirt','minecraft:rooted_dirt','minecraft:grass_block','minecraft:gravel','minecraft:deepslate','minecraft:polished_deepslate','minecraft:deepslate_bricks','minecraft:cracked_deepslate_bricks']);
const conflicts=before.filter(p=>{
 const was=old.get(key(p.x,p.y,p.z)),id=p.block.split('[')[0];
 if(was)return id!==was.block.split('[')[0];
 if(p.y===94)return!terrain.has(id)&&id!=='minecraft:air';
 return id!=='minecraft:air'&&id!=='minecraft:cave_air';
});
if(conflicts.length){fs.writeFileSync(dir+'conflicts.json',JSON.stringify(conflicts,null,2));throw Error('Unexpected current blocks: '+conflicts.length);}
// The eight low edge cells form a short, connected stone footing. Every added cell
// must connect horizontally to existing solid ground through the modeled Y=94 plate.
const groundMap=new Map(ground.map(p=>[key(p.x,p.y,p.z),p]));
const connected=new Set(ground.filter(p=>!p.block.endsWith(':air')).map(p=>key(p.x,p.y,p.z)));
let change=true;while(change){change=false;for(const p of ground){const k=key(p.x,p.y,p.z);if(connected.has(k)||!M.has(k))continue;if(D.some(([a,b])=>connected.has(key(p.x+a,p.y,p.z+b)))){connected.add(k);change=true;}}}
const unsupported=ground.filter(p=>p.block.includes('wool')||!connected.has(key(p.x,p.y,p.z)));
if(unsupported.length){fs.writeFileSync(dir+'unsupported.json',JSON.stringify(unsupported,null,2));throw Error('Disconnected foundation or wool path under tower');}
fs.writeFileSync(dir+'foundation-check.json',JSON.stringify({checked:ground.length,connected:connected.size,plateOverLowGround:ground.filter(p=>p.block.endsWith(':air')).length}));
console.log('Preflight passed; footing connected to solid ground and full before snapshot saved.');
patch.sort((a,b)=>(a.block==='minecraft:air'?-1:0)-(b.block==='minecraft:air'?-1:0)||a.y-b.y);
for(let i=0;i<patch.length;i+=2500){const result=await api('set_blocks',{blocks:patch.slice(i,i+2500)});fs.appendFileSync(dir+'writes.jsonl',JSON.stringify({offset:i,time:new Date().toISOString(),result})+'\n');console.log('Placed',Math.min(i+2500,patch.length),'/',patch.length);}
console.log('COMPLETE');
