import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/island-halloween-festival-20261024/';fs.mkdirSync(dir,{recursive:true});
const castle=JSON.parse(fs.readFileSync('tools/mcwws-build-bridge-mcp/island-gate-fix-20261021/plan.json'));
const oldSets=['island-halloween-20261024','island-halloween-rich-20261024'].map(s=>JSON.parse(fs.readFileSync('tools/mcwws-build-bridge-mcp/'+s+'/plan.json')));
const key=(x,y,z)=>`${x},${y},${z}`,M=new Map([...castle.blocks,...oldSets.flatMap(s=>s.blocks)].map(p=>[key(p.x,p.y,p.z),p]));
const stones=new Set(['minecraft:polished_deepslate','minecraft:deepslate_bricks','minecraft:cracked_deepslate_bricks','minecraft:deepslate_tiles','minecraft:stone_bricks']);
const reserved=new Set(oldSets.flatMap(s=>s.groups.flatMap(g=>(g.empty||[]).map(p=>key(p.x,p.y,p.z)))));
const protectedHead=new Set();for(const p of castle.stairSteps)for(const h of [1,2])protectedHead.add(key(p.x,p.y+h,p.z));for(const p of castle.landings)for(const h of [0,1])protectedHead.add(key(p.x,p.y+h,p.z));
const dirs=[{face:'east',dx:1,dz:0},{face:'south',dx:0,dz:1},{face:'west',dx:-1,dz:0},{face:'north',dx:0,dz:-1}];
const facing=(x,z)=>Math.abs(x)>=Math.abs(z)?dirs[x>0?0:2]:dirs[z>0?1:3];
const all=[...M.values()],groups=[];
function group(kind,blocks,supports=[],empty=[]){
 for(const p of [...blocks,...empty]){const k=key(p.x,p.y,p.z);if(p.x< -388||p.x> -304||p.z<2736||p.z>2820||p.y<95||p.y>131||M.has(k)||reserved.has(k)||protectedHead.has(k))return false;}
 for(const p of supports)if(!stones.has(M.get(key(p.x,p.y,p.z))?.block))return false;
 for(const p of [...blocks,...empty])reserved.add(key(p.x,p.y,p.z));groups.push({kind,blocks,supports,empty});return true;
}
const pots=['potted_wither_rose','potted_dead_bush','potted_red_mushroom','potted_crimson_fungus','potted_warped_fungus'];
// The roof is the principal focus: a full circuit of varied parapet-top ornaments.
for(let ring=0;ring<2;ring++)for(let i=0;i<36;i++){
 const a=(i+.2)*Math.PI*2/36,nx=Math.cos(a),nz=Math.sin(a),dir=facing(nx,nz),desired=ring?33:39;
 const list=all.filter(p=>{const x=p.x+346,z=p.z-2778,r=Math.hypot(x,z);return[119,120].includes(p.y)&&stones.has(p.block)&&Math.abs(r-desired)<1.1&&Math.abs(Math.atan2(Math.sin(Math.atan2(z,x)-a),Math.cos(Math.atan2(z,x)-a)))<.10;}).sort((p,q)=>Math.hypot(p.x+346-desired*nx,p.z-2778-desired*nz)-Math.hypot(q.x+346-desired*nx,q.z-2778-desired*nz));
 for(const p of list){
  const x=p.x,y=p.y+1,z=p.z,type=i%12;let block,extra=[];
  if(type===0)block=`jack_o_lantern[facing=${dir.face}]`;
  else if(type===1){block=`carved_pumpkin[facing=${dir.face}]`;extra=[{x,y:y+1,z,block:'minecraft:black_candle[candles=3,lit=true,waterlogged=false]'}];}
  else if(type===2)block='skeleton_skull[rotation=4]';
  else if(type===3)block='wither_skeleton_skull[rotation=12]';
  else if(type===4)block='soul_lantern[hanging=false,waterlogged=false]';
  else if(type>=5&&type<=7)block=`${['orange','purple','red'][type-5]}_candle[candles=${[3,4,2][type-5]},lit=true,waterlogged=false]`;
  else block=pots[(type-8)%pots.length];
  if(group('roof_parapet_ornament',[{x,y,z,block:'minecraft:'+block},...extra],[p]))break;
 }
}
// Small two-tile witchcraft and harvest stations lie at the edge, leaving the middle lane free.
for(let i=0;i<16;i++){
 const a=(i+.55)*Math.PI*2/16,nx=Math.cos(a),nz=Math.sin(a),r=i%2?34.4:37.7,dir=facing(nx,nz);
 const list=all.filter(p=>{
  if(p.y!==118||!stones.has(p.block))return false;const x=p.x+346,z=p.z-2778,d=Math.hypot(x,z);
  return(i%2?d<35:d>37)&&Math.abs(d-r)<1&&Math.abs(Math.atan2(Math.sin(Math.atan2(z,x)-a),Math.cos(Math.atan2(z,x)-a)))<.10;
 }).sort((p,q)=>Math.hypot(p.x+346-r*nx,p.z-2778-r*nz)-Math.hypot(q.x+346-r*nx,q.z-2778-r*nz));
 for(const p of list){
  const tx=-dir.dz,tz=dir.dx,q={x:p.x+tx,y:118,z:p.z+tz};
  const blocks=[{x:p.x,y:119,z:p.z,block:'minecraft:barrel[facing=up,open=false]'},
   {x:p.x,y:120,z:p.z,block:'minecraft:'+(['brewing_stand',...pots][i%6])},
   {x:q.x,y:119,z:q.z,block:i%2?'minecraft:water_cauldron[level=2]':`minecraft:carved_pumpkin[facing=${dir.face}]`}];
  if(i%2===0)blocks.push({x:q.x,y:120,z:q.z,block:'minecraft:orange_candle[candles=3,lit=true,waterlogged=false]'});
  if(group('roof_witchcraft_nook',blocks,[p,q]))break;
 }
}
// Pennants and skull plaques decorate the sides of merlons, outside the roof walking lane.
for(let ring=0;ring<2;ring++)for(let i=0;i<32;i++){
 const a=(i+.65)*2*Math.PI/32,nx=Math.cos(a),nz=Math.sin(a),r=ring?33:39,dir=facing(ring?-nx:nx,ring?-nz:nz);
 const list=all.filter(p=>p.y===120&&stones.has(p.block)&&Math.abs(Math.hypot(p.x+346,p.z-2778)-r)<1.1&&Math.abs(Math.atan2(Math.sin(Math.atan2(p.z-2778,p.x+346)-a),Math.cos(Math.atan2(p.z-2778,p.x+346)-a)))<.13).sort((p,q)=>Math.hypot(p.x+346-r*nx,p.z-2778-r*nz)-Math.hypot(q.x+346-r*nx,q.z-2778-r*nz));
 for(const p of list){const x=p.x+dir.dx,z=p.z+dir.dz;
  if(i%2===0){if(group('roof_merlon_pennant',[{x,y:120,z,block:`minecraft:${['orange','black','purple'][i%3]}_wall_banner[facing=${dir.face}]`}],[p],[{x,y:119,z}]))break;}
  else if(group('roof_skull_plaque',[{x,y:120,z,block:`minecraft:${i%4===1?'skeleton_wall_skull':'wither_skeleton_wall_skull'}[facing=${dir.face}]`}],[p]))break;
 }
}
function rotate(x,z,r){for(let i=0;i<r;i++)[x,z]=[-z,x];return[x,z];}
for(let r=0;r<4;r++)for(let level=0;level<3;level++){
 const floor=[102,110,118][level],[dx,dz]=rotate(25,level===1?-11:8,r),x=-346+dx,z=2778+dz;
 group('extra_landing_curio',[{x,y:floor+1,z,block:level===2?'minecraft:water_cauldron[level=2]':'minecraft:barrel[facing=up,open=false]'},...(level===2?[]:[{x,y:floor+2,z,block:'minecraft:'+pots[(r+level)%pots.length]}])],[{x,y:floor,z}]);
}
function candidates(a,y,side){
 const nx=Math.cos(a),nz=Math.sin(a),r=(side==='courtyard'||side==='inner')?33:39,dir=facing(side==='courtyard'||side==='outer'?-nx:nx,side==='courtyard'||side==='outer'?-nz:nz);
 const list=all.filter(p=>{
  if(p.y!==y||!stones.has(p.block)||Math.abs(Math.hypot(p.x+346,p.z-2778)-r)>1.1)return false;
  if(castle.towers.some(t=>Math.hypot(p.x+346-t.x,p.z-2778-t.z)<6))return false;
  return Math.abs(Math.atan2(Math.sin(Math.atan2(p.z-2778,p.x+346)-a),Math.cos(Math.atan2(p.z-2778,p.x+346)-a)))<.14;
 }).sort((p,q)=>Math.hypot(p.x+346-r*nx,p.z-2778-r*nz)-Math.hypot(q.x+346-r*nx,q.z-2778-r*nz));return{list,dir};
}
// Apothecary shelves and skulls add distinct objects inside, in the courtyard and outside.
for(const [level,base]of[95,103,111].entries())for(let sideIndex=0;sideIndex<4;sideIndex++)for(let i=0;i<12;i++){
 const side=['courtyard','inner','outer','outside'][sideIndex],a=(i+.72)*2*Math.PI/12;
 const{list,dir}=candidates(a,base+4,side);
 for(const p of list){const x=p.x+dir.dx,z=p.z+dir.dz,y=p.y;
  if(group('apothecary_wall_shelf',[{x,y,z,block:'minecraft:polished_deepslate_slab[type=top,waterlogged=false]'},{x,y:y+1,z,block:'minecraft:'+pots[(i+sideIndex+level)%pots.length]}],[p]))break;
 }
}
for(const base of[95,103,111])for(let i=0;i<16;i++){
 const{list,dir}=candidates((i+.4)*2*Math.PI/16,base+3,i%2?'inner':'outer');
 for(const p of list)if(group('corridor_skull_plaque',[{x:p.x+dir.dx,y:p.y,z:p.z+dir.dz,block:`minecraft:${i%3?'skeleton_wall_skull':'wither_skeleton_wall_skull'}[facing=${dir.face}]`}],[p]))break;
}
for(const floor of[102,110,118])for(let i=0;i<24;i++){
 const a=(i+.35)*2*Math.PI/24,r=i%2?34:38;
 const list=all.filter(p=>p.y===floor&&stones.has(p.block)&&Math.abs(Math.hypot(p.x+346,p.z-2778)-r)<1&&Math.abs(Math.atan2(Math.sin(Math.atan2(p.z-2778,p.x+346)-a),Math.cos(Math.atan2(p.z-2778,p.x+346)-a)))<.11).sort((p,q)=>Math.hypot(p.x+346-r*Math.cos(a),p.z-2778-r*Math.sin(a))-Math.hypot(q.x+346-r*Math.cos(a),q.z-2778-r*Math.sin(a)));
 for(const p of list)if(group('ceiling_hanging_roots',[{x:p.x,y:floor-1,z:p.z,block:'minecraft:hanging_roots'}],[p]))break;
}
for(let i=0;i<castle.towers.length;i++){
 const t=castle.towers[i],x=-346+t.x-2,z=2778+t.z+2;
 group('tower_apothecary',[{x,y:t.deck+1,z,block:'minecraft:barrel[facing=up,open=false]'}, {x,y:t.deck+2,z,block:'minecraft:brewing_stand'}, {x,y:t.deck+1,z:z-1,block:'minecraft:water_cauldron[level=2]'}],[{x,y:t.deck,z},{x,y:t.deck,z:z-1}]);
}
const counts={};for(const g of groups)counts[g.kind]=(counts[g.kind]||0)+1;
fs.writeFileSync(dir+'proposed-plan.json',JSON.stringify({groups,counts}));console.log('Proposed',JSON.stringify(counts),'blocks',groups.reduce((n,g)=>n+g.blocks.length,0));
if(!process.argv.includes('--apply'))process.exit(0);
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(e,body){const r=await fetch('http://127.0.0.1:8765/'+e,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...body})});const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));return j;}
async function read(list){let i=0;const out=new Map();await Promise.all(Array.from({length:24},async()=>{while(i<list.length){const p=list[i++];out.set(key(p.x,p.y,p.z),(await api('get_block',{x:p.x,y:p.y,z:p.z})).block);}}));return out;}
const points=new Map();for(const g of groups)for(const p of [...g.blocks,...g.supports,...g.empty])points.set(key(p.x,p.y,p.z),{x:p.x,y:p.y,z:p.z});
const before=await read([...points.values()]),accepted=groups.filter(g=>[...g.blocks,...g.empty].every(p=>before.get(key(p.x,p.y,p.z))==='minecraft:air')&&g.supports.every(p=>stones.has(before.get(key(p.x,p.y,p.z)))));
const blocks=accepted.flatMap(g=>g.blocks);if(!blocks.length)throw Error('No clear ornament sites');
const model=new Map(M);for(const p of blocks)model.set(key(p.x,p.y,p.z),p);
const connectivity=[];
for(const floor of[102,110,118]){
 const walk=new Set([...model.values()].filter(p=>p.y===floor&&stones.has(p.block)&&!model.has(key(p.x,floor+1,p.z))&&!model.has(key(p.x,floor+2,p.z))).map(p=>`${p.x},${p.z}`));
 const anchor=castle.landings.find(p=>p.y===floor+1),start=`${anchor.x},${anchor.z}`,q=[start],seen=new Set(q);
 for(let n=0;n<q.length;n++){const[x,z]=q[n].split(',').map(Number);for(const[a,b]of[[1,0],[-1,0],[0,1],[0,-1]]){const v=`${x+a},${z+b}`;if(walk.has(v)&&!seen.has(v)){seen.add(v);q.push(v);}}}
 const missing=castle.landings.filter(p=>p.y===floor+1&&!seen.has(`${p.x},${p.z}`)),angles=[];
 for(let i=0;i<72;i++){const a=i*Math.PI/36;if(![34,35,36,37,38].some(r=>seen.has(`${-346+Math.round(r*Math.cos(a))},${2778+Math.round(r*Math.sin(a))}`)))angles.push(i*5);}
 if(missing.length||angles.length)throw Error('Roof or gallery circulation obstructed');connectivity.push({floor,fullCircuit:true,fourStairsConnected:true,reachable:seen.size});
}
fs.writeFileSync(dir+'before-'+Date.now()+'.json',JSON.stringify([...before].map(([pos,block])=>({pos,block}))));
fs.writeFileSync(dir+'plan.json',JSON.stringify({groups:accepted,blocks,connectivity}));
const result=await api('set_blocks',{blocks});fs.appendFileSync(dir+'writes.jsonl',JSON.stringify({time:new Date().toISOString(),result})+'\n');console.log('Placed',blocks.length,'blocks,',accepted.length,'groups');
const after=await read(blocks),failures=blocks.filter(p=>{const a=after.get(key(p.x,p.y,p.z));return a.split('[')[0]!==p.block.split('[')[0]||p.block.includes('lit=true')&&!a.includes('lit=true')||p.block.includes('facing=')&&!a.includes(p.block.match(/facing=\w+/)[0]);});
const actualCounts={};for(const g of accepted)actualCounts[g.kind]=(actualCounts[g.kind]||0)+1;
const materials=[...new Set(blocks.map(p=>p.block.split('[')[0]))];
const report={time:new Date().toISOString(),placed:blocks.length,types:materials.length,materials,groups:actualCounts,skipped:groups.length-accepted.length,failures,connectivity};
fs.writeFileSync(dir+'verification.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report));if(failures.length)process.exitCode=1;
