import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/island-castle-refinement-20261020/';
fs.mkdirSync(dir,{recursive:true});
const source=JSON.parse(fs.readFileSync('tools/mcwws-build-bridge-mcp/island-castle-expansion-20261020/plan.json'));
const floor=JSON.parse(fs.readFileSync(dir+'floor-sample.json'));
const key=(x,y,z)=>`${x},${y},${z}`,M=new Map(source.blocks.map(p=>[key(p.x,p.y,p.z),{...p}]));
const old=new Map(source.blocks.map(p=>[key(p.x,p.y,p.z),p]));
function set(dx,y,dz,b){
 if(Math.hypot(dx,dz)>40.001||y<95||y>128)throw Error('Outside approved envelope');
 M.set(key(dx-346,y,dz+2778),{x:dx-346,y,z:dz+2778,block:'minecraft:'+b});
}
const D=[[1,0],[-1,0],[0,1],[0,-1]];
function inverse(x,z,r){for(let i=0;i<r;i++)[x,z]=[z,-x];return [x,z];}
function entry(x,z,base){
 for(let r=0;r<4;r++){
  const [a,c]=inverse(x,z,r);
  if(a>=29&&a<=34&&(base===103?(c>=7&&c<=8):(c>=-12&&c<=-10)))return true;
 }return false;
}
let filled=0,glass=0;const styles={};
for(let x=-39;x<=39;x++)for(let z=-39;z<=39;z++){
 const radius=Math.hypot(x,z);if(radius<33||radius>39)continue;
 const outer=D.some(([a,b])=>Math.hypot(x+a,z+b)>39),inner=D.some(([a,b])=>Math.hypot(x+a,z+b)<33);
 if(!outer&&!inner)continue;
 if(source.towers.some(t=>Math.hypot(x-t.x,z-t.z)<=4.5))continue;
 const a=(Math.atan2(z,x)+2*Math.PI)%(2*Math.PI);
 const s=a/(2*Math.PI)*32,bay=Math.floor(s+.5)%32,phase=((s+.5)%1-.5)*7.07;
 for(let level=0;level<2;level++){
  const base=103+8*level;if(entry(x,z,base))continue;
  // Open bays on opposite faces never coincide. Half of the remaining bays are glazed.
  const style=(bay+level+(inner?2:0))%4;
  styles[`${level}-${inner?'inner':'outer'}-${style}`]=(styles[`${level}-${inner?'inner':'outer'}-${style}`]||0)+1;
  for(let h=0;h<=5;h++){
   const y=base+h,k=key(x-346,y,z+2778),prior=M.get(k);
   // Keep piers, arch stones, doorframes and structural pieces intact.
   if(prior&&!prior.block.startsWith('minecraft:iron_bars'))continue;
   let b=null;
   if(style===0)b='stone_bricks';
   else if(style===1){
    if(h===0)b='polished_deepslate';
    else b='gray_stained_glass';
   }else if(style===2){
    b=h>=2&&h<=4&&Math.abs(phase)<=1.15?'gray_stained_glass':'stone_bricks';
   }else if(h===0)b='polished_deepslate';
   if(b){if(!prior)filled++;if(b.includes('glass'))glass++;set(x,y,z,b);}
  }
 }
}
// Use the measured ground palette, counting stair forms as polished-deepslate material.
const count=floor.counts,total=Object.values(count).reduce((a,b)=>a+b,0);
const polished=(count['minecraft:polished_deepslate']+count['minecraft:polished_deepslate_stairs'])/total;
const brick=count['minecraft:deepslate_bricks']/total;
const cracked=count['minecraft:cracked_deepslate_bricks']/total;
const fullIds=new Set(['minecraft:stone_bricks','minecraft:deepslate_bricks','minecraft:deepslate_tiles','minecraft:polished_deepslate','minecraft:cracked_deepslate_bricks']);
const cubes=[...M.values()].filter(p=>fullIds.has(p.block));
const fixed=cubes.filter(p=>p.block==='minecraft:polished_deepslate');
const body=cubes.filter(p=>p.block!=='minecraft:polished_deepslate');
function hash(p){
 let h=(Math.imul(p.x,73856093)^Math.imul(p.y,19349663)^Math.imul(p.z,83492791))>>>0;
 h=Math.imul(h^(h>>>16),0x7feb352d)>>>0;h=Math.imul(h^(h>>>15),0x846ca68b)>>>0;return(h^(h>>>16))>>>0;
}
body.sort((a,b)=>hash(a)-hash(b)||a.x-b.x||a.y-b.y||a.z-b.z);
const addPolished=Math.max(0,Math.round(cubes.length*polished)-fixed.length);
const brickTarget=Math.round(cubes.length*brick);
for(let i=0;i<body.length;i++)body[i].block=i<addPolished?'minecraft:polished_deepslate':i<addPolished+brickTarget?'minecraft:deepslate_bricks':'minecraft:cracked_deepslate_bricks';
// Keep the half-step geometry while moving every stair and band into the same palette.
for(const p of M.values()){
 if(p.block.startsWith('minecraft:stone_brick_slab'))p.block=p.block.replace('minecraft:stone_brick_slab','minecraft:polished_deepslate_slab');
}
// Explicit pane/bar/wall connections for physics-free Bukkit placement.
for(const p of M.values()){
 const id=p.block.split('[')[0];
 const wall=id==='minecraft:polished_deepslate_wall',pane=id==='minecraft:gray_stained_glass_pane'||id==='minecraft:iron_bars';
 if(!wall&&!pane)continue;
 const sides=[['north',0,-1],['south',0,1],['east',1,0],['west',-1,0]].map(([n,dx,dz])=>`${n}=${M.has(key(p.x+dx,p.y,p.z+dz))?(wall?'low':'true'):(wall?'none':'false')}`);
 p.block=`${id}[${sides.join(',')},${wall?'up=true,':''}waterlogged=false]`;
}
const blocks=[...M.values()],patch=blocks.filter(p=>old.get(key(p.x,p.y,p.z))?.block!==p.block);
const materialCounts={};for(const p of cubes)materialCounts[p.block]=(materialCounts[p.block]||0)+1;
const plan={...source,blocks,patch,palette:{ground:count,target:{polished,brick,cracked},actual:materialCounts},closure:{filled,glass,styles}};
fs.writeFileSync(dir+'plan.json',JSON.stringify(plan));
console.log('PLAN',JSON.stringify({changes:patch.length,newInfill:filled,glazedBlocks:glass,palette:materialCounts}));
if(!process.argv.includes('--apply'))process.exit(0);
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(endpoint,body){
 const r=await fetch('http://127.0.0.1:8765/'+endpoint,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...body})});
 const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));return j;
}
let next=0,done=0;const before=new Array(patch.length);
await Promise.all(Array.from({length:24},async()=>{while(next<patch.length){
 const i=next++,p=patch[i];before[i]={x:p.x,y:p.y,z:p.z,block:(await api('get_block',{x:p.x,y:p.y,z:p.z})).block};if(++done%2500===0)console.log('Snapshot',done,'/',patch.length);
}}));
const conflicts=before.filter(p=>p.block.split('[')[0]!==((old.get(key(p.x,p.y,p.z))?.block)||'minecraft:air').split('[')[0]);
fs.writeFileSync(dir+'before-'+Date.now()+'.json',JSON.stringify(before));
if(conflicts.length){fs.writeFileSync(dir+'conflicts.json',JSON.stringify(conflicts,null,2));throw Error('Unmodeled changes: '+conflicts.length);}
for(let i=0;i<patch.length;i+=3000){
 const result=await api('set_blocks',{blocks:patch.slice(i,i+3000)});
 fs.appendFileSync(dir+'writes.jsonl',JSON.stringify({offset:i,count:Math.min(3000,patch.length-i),time:new Date().toISOString(),result})+'\n');
 console.log('Refined',Math.min(i+3000,patch.length),'/',patch.length);
}
console.log('COMPLETE');
