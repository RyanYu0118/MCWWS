import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/island-halloween-20261024/';
fs.mkdirSync(dir,{recursive:true});
const source=JSON.parse(fs.readFileSync('tools/mcwws-build-bridge-mcp/island-gate-fix-20261021/plan.json'));
const k=(x,y,z)=>`${x},${y},${z}`;
const M=new Map(source.blocks.map(p=>[k(p.x,p.y,p.z),p]));
const stone=new Set(['minecraft:polished_deepslate','minecraft:deepslate_bricks','minecraft:cracked_deepslate_bricks','minecraft:deepslate_tiles','minecraft:stone_bricks']);
const solid=(x,y,z)=>stone.has(M.get(k(x,y,z))?.block);
const air=(x,y,z)=>!M.has(k(x,y,z));
const reserved=new Set(),groups=[];
const dirs=[{face:'east',dx:1,dz:0},{face:'south',dx:0,dz:1},{face:'west',dx:-1,dz:0},{face:'north',dx:0,dz:-1}];
const outward=(nx,nz)=>Math.abs(nx)>=Math.abs(nz)?dirs[nx>0?0:2]:dirs[nz>0?1:3];
function group(kind,placements,supports=[],empty=[]){
 for(const p of [...placements,...empty]){
  if(p.x< -388||p.x> -304||p.z<2736||p.z>2820||p.y<98||p.y>131||reserved.has(k(p.x,p.y,p.z))||!air(p.x,p.y,p.z))return false;
 }
 for(const p of supports)if(!solid(p.x,p.y,p.z))return false;
 for(const p of [...placements,...empty])reserved.add(k(p.x,p.y,p.z));
 groups.push({kind,placements,supports,empty});return true;
}
function pumpkin(back,dir,label){
 const x=back.x+dir.dx,y=back.y,z=back.z+dir.dz;
 return group(label,[{x,y:y-1,z,block:'minecraft:polished_deepslate_slab[type=top,waterlogged=false]'},
  {x,y,z,block:`minecraft:jack_o_lantern[facing=${dir.face}]`}],[back,{x:back.x,y:y-1,z:back.z}]);
}
const gates=[{nx:-1,nz:0},{nx:0,nz:1},{nx:31.5/Math.hypot(31.5,27.5),nz:27.5/Math.hypot(31.5,27.5)},source.gateCenters.northeast];
for(const g of gates)for(const sign of [-1,1]){
 const dir=outward(g.nx,g.nz);
 const choices=[...M.values()].filter(p=>{
  const x=p.x+346,z=p.z-2778,u=x*g.nx+z*g.nz,s=-x*g.nz+z*g.nx;
  return p.y===99&&stone.has(p.block)&&u>=37.5&&u<=39&&s*sign>=2.8&&s*sign<=5&&air(p.x+dir.dx,p.y,p.z+dir.dz);
 }).sort((a,b)=>{
  const score=p=>Math.abs((-(p.x+346)*g.nz+(p.z-2778)*g.nx)-sign*3.8)+Math.abs((p.x+346)*g.nx+(p.z-2778)*g.nz-38);
  return score(a)-score(b);
 });
 for(const p of choices)if(pumpkin(p,dir,'gate_pumpkin_sconce'))break;
}
// Four contour-following bat reliefs, one above each cardinal curtain-wall bay.
const bat=['#..#..#','#######','.#####.'];
for(const dir of dirs){
 const placements=[],supports=[];let valid=true;
 for(let row=0;row<3;row++)for(let col=0;col<7;col++){
  if(bat[row][col]!== '#')continue;
  const lateral=col-3,y=106-row;let back;
  for(let radius=39;radius>=36;radius--){
   const dx=dir.dx*radius-dir.dz*lateral,dz=dir.dz*radius+dir.dx*lateral;
   if(solid(-346+dx,y,2778+dz)&&air(-346+dx+dir.dx,y,2778+dz+dir.dz)){back={x:-346+dx,y,z:2778+dz};break;}
  }
  if(!back){valid=false;break;}
  supports.push(back);placements.push({x:back.x+dir.dx,y,z:back.z+dir.dz,block:row===1&&col===3?'minecraft:purple_concrete':'minecraft:black_concrete'});
 }
 if(valid)group('bat_relief',placements,supports);
}
// Wall banners only attach to solid piers, with free space below for the cloth.
const colors=['orange','black','purple'];
for(let level=0;level<2;level++)for(let i=0;i<12;i++){
 const a=(i+.5)*Math.PI*2/12,nx=Math.cos(a),nz=Math.sin(a),dir=outward(nx,nz),y=level?116:108;
 const candidates=[...M.values()].filter(p=>{
  const x=p.x+346,z=p.z-2778,r=Math.hypot(x,z);
  if(p.y!==y||!stone.has(p.block)||r<37.5||r>39.1||!solid(p.x,y-1,p.z)||!air(p.x+dir.dx,y,p.z+dir.dz)||!air(p.x+dir.dx,y-1,p.z+dir.dz))return false;
  if(source.towers.some(t=>Math.hypot(x-t.x,z-t.z)<6.5))return false;
  return Math.abs(Math.atan2(Math.sin(Math.atan2(z,x)-a),Math.cos(Math.atan2(z,x)-a)))<.15;
 }).sort((p,q)=>Math.hypot(p.x+346-nx*39,p.z-2778-nz*39)-Math.hypot(q.x+346-nx*39,q.z-2778-nz*39));
 for(const p of candidates){
  const pos={x:p.x+dir.dx,y,z:p.z+dir.dz};
  if(group('wall_banner',[{...pos,block:`minecraft:${colors[(i+level)%3]}_wall_banner[facing=${dir.face}]`}],[p],[{...pos,y:y-1}]))break;
 }
}
for(let i=0;i<source.towers.length;i++){
 const t=source.towers[i],dir=outward(t.x,t.z),back={x:-346+t.x+dir.dx*5,y:125,z:2778+t.z+dir.dz*5};
 pumpkin(back,dir,'tower_pumpkin_sconce');
 const b={...back,y:116},pos={x:b.x+dir.dx,y:116,z:b.z+dir.dz};
 group('tower_banner',[{...pos,block:`minecraft:${colors[(i+1)%3]}_wall_banner[facing=${dir.face}]`}],[b],[{...pos,y:115}]);
}
// Small cobwebs cling to upper window/pier edges, entirely outside circulation lanes.
for(const y of [108,116])for(let i=0;i<16;i++){
 const a=(i+.15)*2*Math.PI/16,dir=outward(Math.cos(a),Math.sin(a));
 const choices=[...M.values()].filter(p=>{
  const dx=p.x+346,dz=p.z-2778,r=Math.hypot(dx,dz);
  return p.y===y&&stone.has(p.block)&&r>37.5&&r<=39.1&&air(p.x+dir.dx,y,p.z+dir.dz)&&solid(p.x,y+1,p.z)&&Math.abs(Math.atan2(Math.sin(Math.atan2(dz,dx)-a),Math.cos(Math.atan2(dz,dx)-a)))<.12;
 }).sort((p,q)=>Math.hypot(p.x+346-39*Math.cos(a),p.z-2778-39*Math.sin(a))-Math.hypot(q.x+346-39*Math.cos(a),q.z-2778-39*Math.sin(a)));
 for(const p of choices)if(group('corner_cobweb',[{x:p.x+dir.dx,y,z:p.z+dir.dz,block:'minecraft:cobweb'}],[p,{x:p.x,y:y+1,z:p.z}]))break;
}
const counts={};for(const g of groups)counts[g.kind]=(counts[g.kind]||0)+1;
fs.writeFileSync(dir+'proposed-plan.json',JSON.stringify({groups,counts}));
console.log('Proposed decorations',JSON.stringify(counts),'blocks',groups.reduce((n,g)=>n+g.placements.length,0));
if(!process.argv.includes('--apply'))process.exit(0);
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(e,body){const r=await fetch('http://127.0.0.1:8765/'+e,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...body})});const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));return j;}
const points=new Map();for(const g of groups)for(const p of [...g.placements,...g.supports,...g.empty])points.set(k(p.x,p.y,p.z),{x:p.x,y:p.y,z:p.z});
const list=[...points.values()],live=new Map();let next=0;
await Promise.all(Array.from({length:24},async()=>{while(next<list.length){const p=list[next++];live.set(k(p.x,p.y,p.z),(await api('get_block',p)).block);}}));
const accepted=groups.filter(g=>[...g.placements,...g.empty].every(p=>live.get(k(p.x,p.y,p.z))==='minecraft:air')&&g.supports.every(p=>stone.has(live.get(k(p.x,p.y,p.z)))));
const skipped=groups.filter(g=>!accepted.includes(g));
const blocks=accepted.flatMap(g=>g.placements);
if(!blocks.length)throw Error('No decoration locations remained clear');
fs.writeFileSync(dir+'before-'+Date.now()+'.json',JSON.stringify([...live].map(([pos,block])=>({pos,block}))));
fs.writeFileSync(dir+'plan.json',JSON.stringify({groups:accepted,skipped,blocks}));
const result=await api('set_blocks',{blocks});fs.appendFileSync(dir+'writes.jsonl',JSON.stringify({time:new Date().toISOString(),result})+'\n');
console.log('Placed',blocks.length,'blocks;',accepted.length,'groups;',skipped.length,'groups skipped',JSON.stringify(result));
let i=0;const failures=[];
await Promise.all(Array.from({length:24},async()=>{while(i<blocks.length){const p=blocks[i++],actual=(await api('get_block',{x:p.x,y:p.y,z:p.z})).block;if(actual.split('[')[0]!==p.block.split('[')[0]||p.block.includes('facing=')&&!actual.includes(p.block.match(/facing=\w+/)[0]))failures.push({...p,actual});}}));
const actualCounts={};for(const g of accepted)actualCounts[g.kind]=(actualCounts[g.kind]||0)+1;
const report={time:new Date().toISOString(),placed:blocks.length,groups:actualCounts,skipped:skipped.map(g=>g.kind),failures};
fs.writeFileSync(dir+'verification.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report));
if(failures.length)process.exitCode=1;
