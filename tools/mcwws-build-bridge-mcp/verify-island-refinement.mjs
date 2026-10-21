import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/island-castle-refinement-20261020/';
const p=JSON.parse(fs.readFileSync(dir+'plan.json'));
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
const k=(x,y,z)=>`${x},${y},${z}`,M=new Map(p.blocks.map(v=>[k(v.x,v.y,v.z),v]));
const targets=new Map();
function expect(x,y,z,block='minecraft:air'){targets.set(k(x,y,z),{x,y,z,block});}
for(const v of p.patch)expect(v.x,v.y,v.z,v.block);
for(const v of p.patch)if(v.block==='minecraft:air')expect(v.x,v.y,v.z);
for(const s of p.stairSteps)for(let h=1;h<=2;h++)expect(s.x,s.y+h,s.z);
// Validate the complete openings in the upper two galleries, not just the frames.
for(const base of [103,111])for(let x=-39;x<=39;x++)for(let z=-39;z<=39;z++){
 const r=Math.hypot(x,z);if(r<33||r>39)continue;
 const boundary=[[1,0],[-1,0],[0,1],[0,-1]].some(([a,b])=>Math.hypot(x+a,z+b)>39||Math.hypot(x+a,z+b)<33);
 if(!boundary||p.towers.some(t=>Math.hypot(x-t.x,z-t.z)<=4.5))continue;
 for(let y=base+1;y<base+6;y++)if(!M.has(k(x-346,y,z+2778)))expect(x-346,y,z+2778);
}
// Cardinal entrance tunnels retain a five-wide, four-high route to the courtyard.
for(let v=-3;v<=1;v++)for(let u=22;u<=39;u++)for(let y=95;y<=98;y++){
 expect(-346-u,y,2778+v);expect(-346+v,y,2778+u);
}
const n=Math.hypot(31.5,27.5);for(let u=23;u<=39;u++)for(let y=95;y<=98;y++)expect(-346+Math.round(u*31.5/n),y,2778+Math.round(u*27.5/n));
// Five-air width at a clean cross section on all three levels.
for(const y of [96,104,112])for(let z=-38;z<=-34;z++)expect(-346,y,z+2778);
// Clearance above every main-gallery floor along the center of the circulation lane.
for(const floor of [102,110,118])for(let i=0;i<72;i++){
 const a=i*Math.PI/36,x=-346+Math.round(36*Math.cos(a)),z=2778+Math.round(36*Math.sin(a));
 for(let h=1;h<=2;h++)if(!M.has(k(x,floor+h,z)))expect(x,floor+h,z);
}
const checks=[...targets.values()];let next=0,done=0,failures=[];
function matches(a,b){if(a.split('[')[0]!==b.split('[')[0])return false;return !b.includes('[')||b.split('[')[1].replace(']','').split(',').every(f=>a.includes(f));}
await Promise.all(Array.from({length:24},async()=>{
 while(next<checks.length){
  const v=checks[next++];const r=await fetch('http://127.0.0.1:8765/get_block',{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',x:v.x,y:v.y,z:v.z})});
  const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));
  if(!matches(j.block,v.block))failures.push({...v,actual:j.block});
  if(++done%3000===0)console.log('Verified',done,'/',checks.length);
 }
}));
const report={time:new Date().toISOString(),checked:checks.length,failures};
fs.writeFileSync(dir+'verification.json',JSON.stringify(report,null,2));
console.log(JSON.stringify({checked:checks.length,failures:failures.length,examples:failures.slice(0,20)}));
if(failures.length)process.exitCode=1;
