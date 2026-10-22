import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/island-gate-fix-20261021/';
const castle=JSON.parse(fs.readFileSync('tools/mcwws-build-bridge-mcp/island-castle-pointed-towers-20261020/plan.json'));
const desired=JSON.parse(fs.readFileSync(dir+'underlay-refined/plan.json'));
const key=(x,y,z)=>`${x},${y},${z}`;
const old=new Map(castle.blocks.map(p=>[key(p.x,p.y,p.z),p]));
const template=new Map(desired.blocks.map(p=>[key(p.x,p.y,p.z),p]));
const edits=new Map(),M=new Map(castle.blocks.map(p=>[key(p.x,p.y,p.z),{...p}]));
const n=Math.hypot(36,22),nx=36/n,nz=-22/n;
const masonry=new Set(['minecraft:polished_deepslate','minecraft:deepslate_bricks','minecraft:cracked_deepslate_bricks','minecraft:stone_bricks','minecraft:deepslate_tiles']);
function edit(x,y,z,block,ramp=false){
 if(x< -385||x> -305||z<2748||z>2817||y<93||y>101)throw Error('Outside local repair envelope');
 const p={x,y,z,block,ramp};edits.set(key(x,y,z),p);
 if(block==='minecraft:air')M.delete(key(x,y,z));else M.set(key(x,y,z),{x,y,z,block});
}
for(let dx=-39;dx<=40;dx++)for(let dz=-29;dz<=39;dz++){
 const u=dx*nx+dz*nz,s=-dx*nz+dz*nx;
 const west=dx<=-22&&Math.abs(dz)<=6;
 const south=dz>=22&&Math.abs(dx)<=6;
 const ne=u>=31&&u<=40&&Math.abs(s)<=5.8;
 if(!west&&!south&&!ne)continue;
 for(let y=95;y<=101;y++){
  const x=-346+dx,z=2778+dz,k=key(x,y,z),prior=old.get(k),want=template.get(k);
  let b=want?.block??'minecraft:air';
  // Retain the existing texture wherever both versions call for a full masonry cube.
  if(prior&&masonry.has(prior.block)&&masonry.has(b))b=prior.block;
  if((prior?.block??'minecraft:air')!==b)edit(x,y,z,b);
 }
}
const rampFloors=[];
// A five-wide half-step ramp spans the two-block rise from the Y=92 wool road.
for(let dx=28;dx<=41;dx++)for(let dz=-29;dz<=-14;dz++){
 const u=dx*nx+dz*nz,s=-dx*nz+dz*nx;if(u<39||u>43.5||Math.abs(s)>2.5)continue;
 const x=-346+dx,z=2778+dz;
 let floorY,material;
 if(u<39.5){floorY=94;material='minecraft:polished_deepslate';}
 else if(u<40.5){floorY=94;material='minecraft:polished_deepslate_slab[type=bottom,waterlogged=false]';}
 else if(u<41.5){floorY=93;material='minecraft:polished_deepslate';}
 else if(u<42.5){floorY=93;material='minecraft:polished_deepslate_slab[type=bottom,waterlogged=false]';}
 else floorY=92;
 if(floorY>92){edit(x,floorY,z,material,true);rampFloors.push({x,y:floorY,z,block:material});}
 for(let y=floorY+1;y<=97;y++)edit(x,y,z,'minecraft:air',true);
}
const patch=[...edits.values()];
const plan={...castle,blocks:[...M.values()],patch,rampFloors,gateCenters:{west:{axis:'z',coordinate:2778},south:{axis:'x',coordinate:-346},northeast:{nx,nz}},repairBounds:{x:[-385,-305],y:[93,101],z:[2748,2817]}};
fs.writeFileSync(dir+'plan.json',JSON.stringify(plan));
console.log('Repair plan',JSON.stringify({candidates:patch.length,rampFloors:rampFloors.length,gateCenters:plan.gateCenters}));
if(!process.argv.includes('--apply'))process.exit(0);
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(e,body){const r=await fetch('http://127.0.0.1:8765/'+e,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...body})});const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));return j;}
async function read(points){let i=0;const out=new Array(points.length);await Promise.all(Array.from({length:24},async()=>{while(i<points.length){const n=i++,p=points[n];out[n]={...p,actual:(await api('get_block',{x:p.x,y:p.y,z:p.z})).block};}}));return out;}
const before=await read(patch);fs.writeFileSync(dir+'before-'+Date.now()+'.json',JSON.stringify(before));
const terrain=new Set(['minecraft:air','minecraft:cave_air','minecraft:stone','minecraft:calcite','minecraft:andesite','minecraft:dirt','minecraft:coarse_dirt','minecraft:rooted_dirt','minecraft:grass_block','minecraft:short_grass','minecraft:gravel',...masonry]);
const conflicts=before.filter(p=>{
 const expected=old.get(key(p.x,p.y,p.z))?.block??'minecraft:air',id=p.actual.split('[')[0];
 if(!p.ramp)return id!==expected.split('[')[0];
 return!terrain.has(id)&&id!=='minecraft:polished_deepslate_stairs'&&id!=='minecraft:polished_deepslate_slab';
});
if(conflicts.length){fs.writeFileSync(dir+'conflicts.json',JSON.stringify(conflicts,null,2));throw Error('Unexpected blocks in local repair: '+conflicts.length);}
const writes=before.filter(p=>p.actual!==p.block).map(({x,y,z,block})=>({x,y,z,block}));
const result=await api('set_blocks',{blocks:writes});fs.appendFileSync(dir+'writes.jsonl',JSON.stringify({time:new Date().toISOString(),result})+'\n');
console.log('Repaired',writes.length,JSON.stringify(result));
const after=await read(patch),failures=after.filter(p=>p.actual.split('[')[0]!==p.block.split('[')[0]||p.block.includes('type=bottom')&&!p.actual.includes('type=bottom'));
const clearance=[];
for(let v=-2;v<=2;v++)for(let u=22;u<=38;u++)for(let y=95;y<=97;y++){
 clearance.push({x:-346-u,y,z:2778+v});clearance.push({x:-346+v,y,z:2778+u});
}
for(let u=31;u<=39;u++)for(let v=-1;v<=1;v++)for(let y=95;y<=98;y++)clearance.push({x:-346+Math.round(u*nx-v*nz),y,z:2778+Math.round(u*nz+v*nx)});
const check=await read(clearance),blocked=check.filter(p=>p.actual!=='minecraft:air');
const report={time:new Date().toISOString(),changed:writes.length,verified:after.length,failures,clearanceChecks:clearance.length,blocked};
fs.writeFileSync(dir+'verification.json',JSON.stringify(report,null,2));console.log(JSON.stringify({changed:writes.length,failures:failures.length,clearanceChecks:clearance.length,blocked:blocked.slice(0,10)}));
if(failures.length||blocked.length)process.exitCode=1;
