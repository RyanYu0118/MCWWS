import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/island-halloween-rich-20261024/';
fs.mkdirSync(dir,{recursive:true});
const castle=JSON.parse(fs.readFileSync('tools/mcwws-build-bridge-mcp/island-gate-fix-20261021/plan.json'));
const previous=JSON.parse(fs.readFileSync('tools/mcwws-build-bridge-mcp/island-halloween-20261024/plan.json'));
const key=(x,y,z)=>`${x},${y},${z}`,M=new Map([...castle.blocks,...previous.blocks].map(p=>[key(p.x,p.y,p.z),p]));
const stone=new Set(['minecraft:polished_deepslate','minecraft:deepslate_bricks','minecraft:cracked_deepslate_bricks','minecraft:deepslate_tiles','minecraft:stone_bricks']);
const full=p=>p&&stone.has(p.block);
const reserved=new Set(previous.groups.flatMap(g=>g.empty.map(p=>key(p.x,p.y,p.z))));
const protectedHead=new Set();for(const p of castle.stairSteps)for(const h of [1,2])protectedHead.add(key(p.x,p.y+h,p.z));
for(const p of castle.landings)for(const h of [0,1])protectedHead.add(key(p.x,p.y+h,p.z));
const dirs=[{face:'east',dx:1,dz:0},{face:'south',dx:0,dz:1},{face:'west',dx:-1,dz:0},{face:'north',dx:0,dz:-1}];
const face=(nx,nz)=>Math.abs(nx)>=Math.abs(nz)?dirs[nx>0?0:2]:dirs[nz>0?1:3];
function rotate(x,z,r){for(let i=0;i<r;i++)[x,z]=[-z,x];return[x,z];}
const groups=[];
function group(kind,blocks,supports=[],empty=[]){
 for(const p of [...blocks,...empty]){
  const k=key(p.x,p.y,p.z);
  if(p.x< -388||p.x> -304||p.z<2736||p.z>2820||p.y<95||p.y>131||M.has(k)||reserved.has(k)||protectedHead.has(k))return false;
 }
 for(const s of supports)if(!s.unmodeled&&!full(M.get(key(s.x,s.y,s.z))))return false;
 for(const p of [...blocks,...empty])reserved.add(key(p.x,p.y,p.z));
 groups.push({kind,blocks,supports,empty});return true;
}
// Four directions x three storeys: small displays occupy the dead corner of each landing.
for(let r=0;r<4;r++)for(let level=0;level<3;level++){
 const floor=[102,110,118][level],localZ=level===1?-11:8,[dx,dz]=rotate(24,localZ,r),x=-346+dx,z=2778+dz;
 group('landing_corner_display',[
  {x,y:floor+1,z,block:`minecraft:jack_o_lantern[facing=${dirs[r].face}]`},
  {x,y:floor+2,z,block:level===1?'minecraft:skeleton_skull[rotation=4]':`minecraft:${level===2?'purple':'black'}_candle[candles=3,lit=true,waterlogged=false]`},
 ],[{x,y:floor,z}]);
}
for(let r=0;r<4;r++){
 const[dx,dz]=rotate(28,-10,r),x=-346+dx,z=2778+dz;
 group('stair_foot_display',[{x,y:95,z,block:`minecraft:jack_o_lantern[facing=${dirs[r].face}]`},{x,y:96,z,block:'minecraft:black_candle[candles=3,lit=true,waterlogged=false]'}],[{x,y:94,z,unmodeled:true}]);
}
const all=[...M.values()];
function candidates(angle,y,side){
 const nx=Math.cos(angle),nz=Math.sin(angle);
 const dir=face(side==='courtyard'?-nx:side==='corridor_outer'?-nx:nx,side==='courtyard'?-nz:side==='corridor_outer'?-nz:nz);
 const list=all.filter(p=>{
  const dx=p.x+346,dz=p.z-2778,r=Math.hypot(dx,dz);
  if(p.y!==y||!stone.has(p.block))return false;
  if(side==='courtyard'||side==='corridor_inner'){if(r<32.9||r>34.1)return false;}
  else if(r<37.5||r>39.1)return false;
  if(castle.towers.some(t=>Math.hypot(dx-t.x,dz-t.z)<6))return false;
  const a=Math.atan2(dz,dx);return Math.abs(Math.atan2(Math.sin(a-angle),Math.cos(a-angle)))<.18;
 }).sort((p,q)=>Math.hypot(p.x+346-nx*(side==='courtyard'||side==='corridor_inner'?33:39),p.z-2778-nz*(side==='courtyard'||side==='corridor_inner'?33:39))-Math.hypot(q.x+346-nx*(side==='courtyard'||side==='corridor_inner'?33:39),q.z-2778-nz*(side==='courtyard'||side==='corridor_inner'?33:39)));
 return{list,dir};
}
function banner(back,dir,color,kind){const p={x:back.x+dir.dx,y:back.y,z:back.z+dir.dz};return group(kind,[{...p,block:`minecraft:${color}_wall_banner[facing=${dir.face}]`}],[back],[{...p,y:p.y-1}]);}
function pumpkin(back,dir,kind){const x=back.x+dir.dx,y=back.y,z=back.z+dir.dz;return group(kind,[{x,y:y-1,z,block:'minecraft:polished_deepslate_slab[type=top,waterlogged=false]'}, {x,y,z,block:`minecraft:jack_o_lantern[facing=${dir.face}]`}],[back,{x:back.x,y:y-1,z:back.z}]);}
function hanging(back,dir,kind){
 const x=back.x+dir.dx,y=back.y,z=back.z+dir.dz;
 const toward={east:'west',west:'east',north:'south',south:'north'}[dir.face];
 const bars=['east','west','north','south'].map(s=>`${s}=${s===toward?'true':'false'}`).join(',');
 return group(kind,[{x,y,z,block:`minecraft:iron_bars[${bars},waterlogged=false]`},{x,y:y-1,z,block:'minecraft:iron_chain[axis=y,waterlogged=false]'},{x,y:y-2,z,block:'minecraft:lantern[hanging=true,waterlogged=false]'}],[back]);
}
// Courtyard-facing decorations are distinct from the earlier exterior-only pass.
for(const [level,base]of [95,103,111].entries())for(let i=0;i<12;i++){
 const a=(i+.4)*Math.PI*2/12;
 const {list,dir}=candidates(a,base+5,'courtyard');
 for(const p of list)if(banner(p,dir,['orange','purple','black'][(i+level)%3],'courtyard_banner'))break;
}
for(const [level,base]of [95,103,111].entries())for(let i=0;i<8;i++){
 const a=(i+.55)*Math.PI*2/8;
 const {list,dir}=candidates(a,base+4,'courtyard');
 for(const p of list)if(pumpkin(p,dir,'courtyard_pumpkin_sconce'))break;
}
// Within the hollow wall, fixtures hang above head level and outside stair clearances.
for(const base of [95,103,111])for(let i=0;i<12;i++){
 const side=i%2?'corridor_inner':'corridor_outer',a=(i+.2)*Math.PI*2/12;
 const {list,dir}=candidates(a,base+6,side);
 for(const p of list)if(hanging(p,dir,'corridor_chain_lantern'))break;
}
for(const base of [95,103,111])for(let i=0;i<8;i++){
 const side=i%2?'corridor_outer':'corridor_inner',a=(i+.65)*Math.PI*2/8;
 const {list,dir}=candidates(a,base+5,side);
 for(const p of list)if(banner(p,dir,['purple','orange','black'][i%3],'corridor_banner'))break;
}
// Additional exterior pendants provide a second height and silhouette in the facade.
for(const base of [95,103,111])for(let i=0;i<8;i++){
 const {list,dir}=candidates((i+.05)*Math.PI*2/8,base+6,'outside');
 for(const p of list)if(hanging(p,dir,'exterior_chain_lantern'))break;
}
for(let i=0;i<12;i++){
 const {list,dir}=candidates((i+.6)*Math.PI*2/12,99,'outside');
 for(const p of list)if(pumpkin(p,dir,'exterior_pumpkin_sconce'))break;
}
// Webbing is high on corridor corners; none is placed at walking or head height.
for(const base of [95,103,111])for(let i=0;i<12;i++){
 const {list,dir}=candidates((i+.9)*Math.PI*2/12,base+5,i%2?'corridor_inner':'corridor_outer');
 for(const p of list){const pos={x:p.x+dir.dx,y:p.y,z:p.z+dir.dz};if(group('high_corridor_cobweb',[{...pos,block:'minecraft:cobweb'}],[p]))break;}
}
for(let r=0;r<4;r++)for(const [lx,lz,y,bx,bz]of [[24,9,107,23,9],[23,8,108,23,9],[31,-11,115,32,-11]]){
 const[dx,dz]=rotate(lx,lz,r),[sx,sz]=rotate(bx,bz,r);
 group('stair_corner_cobweb',[{x:-346+dx,y,z:2778+dz,block:'minecraft:cobweb'}],[{x:-346+sx,y,z:2778+sz}]);
}
const counts={};for(const g of groups)counts[g.kind]=(counts[g.kind]||0)+1;
fs.writeFileSync(dir+'proposed-plan.json',JSON.stringify({groups,counts}));
console.log('Plan',JSON.stringify(counts),'blocks',groups.reduce((n,g)=>n+g.blocks.length,0));
if(!process.argv.includes('--apply'))process.exit(0);
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(e,body){const r=await fetch('http://127.0.0.1:8765/'+e,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...body})});const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));return j;}
async function read(points){let i=0;const out=new Map();await Promise.all(Array.from({length:24},async()=>{while(i<points.length){const p=points[i++];out.set(key(p.x,p.y,p.z),(await api('get_block',{x:p.x,y:p.y,z:p.z})).block);}}));return out;}
const points=new Map();for(const g of groups)for(const p of [...g.blocks,...g.empty,...g.supports])points.set(key(p.x,p.y,p.z),{x:p.x,y:p.y,z:p.z});
const live=await read([...points.values()]);
const accepted=groups.filter(g=>[...g.blocks,...g.empty].every(p=>live.get(key(p.x,p.y,p.z))==='minecraft:air')&&g.supports.every(p=>stone.has(live.get(key(p.x,p.y,p.z)))));
const skipped=groups.filter(g=>!accepted.includes(g)),blocks=accepted.flatMap(g=>g.blocks);
if(!blocks.length)throw Error('No clear decoration sites');
// Check the entire ring and all stair landings against the added floor displays.
const model=new Map(M);for(const p of blocks)model.set(key(p.x,p.y,p.z),p);
const connectivity=[];
for(const floor of [102,110,118]){
 const walk=new Set([...model.values()].filter(p=>p.y===floor&&stone.has(p.block)&&!model.has(key(p.x,floor+1,p.z))&&!model.has(key(p.x,floor+2,p.z))).map(p=>`${p.x},${p.z}`));
 const anchor=castle.landings.find(p=>p.y===floor+1),start=`${anchor.x},${anchor.z}`,queue=[start],seen=new Set(queue);
 for(let n=0;n<queue.length;n++){const[x,z]=queue[n].split(',').map(Number);for(const[a,b]of[[1,0],[-1,0],[0,1],[0,-1]]){const q=`${x+a},${z+b}`;if(walk.has(q)&&!seen.has(q)){seen.add(q);queue.push(q);}}}
 const missing=castle.landings.filter(p=>p.y===floor+1&&!seen.has(`${p.x},${p.z}`));
 const radial=[];for(let i=0;i<72;i++){const a=i*Math.PI/36;if(![34,35,36,37,38].some(r=>seen.has(`${-346+Math.round(r*Math.cos(a))},${2778+Math.round(r*Math.sin(a))}`)))radial.push(i*5);}
 if(missing.length||radial.length)throw Error('Decor blocks a circulation route');
 connectivity.push({floor,reachable:seen.size,allFourLandingsConnected:true,fullRingConnected:true});
}
fs.writeFileSync(dir+'before-'+Date.now()+'.json',JSON.stringify([...live].map(([pos,block])=>({pos,block}))));
fs.writeFileSync(dir+'plan.json',JSON.stringify({groups:accepted,skipped,blocks,connectivity}));
const result=await api('set_blocks',{blocks});fs.appendFileSync(dir+'writes.jsonl',JSON.stringify({time:new Date().toISOString(),result})+'\n');console.log('Placed',blocks.length,'blocks;',accepted.length,'groups;',skipped.length,'skipped');
const after=await read(blocks),failures=blocks.filter(p=>{const a=after.get(key(p.x,p.y,p.z));return a.split('[')[0]!==p.block.split('[')[0]||p.block.includes('facing=')&&!a.includes(p.block.match(/facing=\w+/)[0])||p.block.includes('lit=true')&&!a.includes('lit=true');});
const reportCounts={};for(const g of accepted)reportCounts[g.kind]=(reportCounts[g.kind]||0)+1;
const report={time:new Date().toISOString(),placed:blocks.length,groups:reportCounts,skipped:skipped.map(g=>g.kind),failures,connectivity};
fs.writeFileSync(dir+'verification.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report));if(failures.length)process.exitCode=1;
