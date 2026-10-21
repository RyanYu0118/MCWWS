import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/island-wall-20261020/';
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(e,body){const r=await fetch('http://127.0.0.1:8765/'+e,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...body})});const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));return j;}
const key=p=>`${p.x},${p.y},${p.z}`,id=b=>b.split('[')[0];
const old=new Map(JSON.parse(fs.readFileSync(dir+'plan-first-pass.json')).blocks.map(p=>[key(p),p]));
const final=new Map(JSON.parse(fs.readFileSync(dir+'plan.json')).blocks.map(p=>[key(p),p]));
async function read(points){let i=0,done=0;const out=[];await Promise.all(Array.from({length:24},async()=>{while(i<points.length){const p=points[i++];out.push({...p,actual:(await api('get_block',{x:p.x,y:p.y,z:p.z})).block});if(++done%2000===0)console.log('Checked',done,'/',points.length);}}));return out;}
function matches(actual,expected){
 if(id(actual)!==id(expected))return false;
 if(!expected.includes('['))return true;
 const fields=expected.split('[')[1].replace(']','').split(',');return fields.every(f=>actual.includes(f));
}
if(process.argv.includes('--fix')){
 const patch=[];
 for(const [k,p]of old)if(!final.has(k))patch.push({...p,block:'minecraft:air'});
 for(const [k,p]of final)if(!old.has(k)||old.get(k).block!==p.block)patch.push(p);
 const before=await read(patch);
 for(const p of before){const prev=old.get(key(p));if(!matches(p.actual,prev?.block??'minecraft:air'))throw Error('Concurrent edit at '+key(p)+' '+p.actual);}
 fs.writeFileSync(dir+'refinement-before.json',JSON.stringify(before));
 console.log('Refinement',patch.length,await api('set_blocks',{blocks:patch}));
}
const results=await read([...final.values()]);
const failed=results.filter(p=>!matches(p.actual,p.block));
const checks=[];
// Exactly five air blocks between north inner/outer leaves at standing height.
for(let dz=-38;dz<=-34;dz++)for(let y of [95,97,100])checks.push({x:-346,y,z:2778+dz,block:'minecraft:air'});
// The west and south arches have five-wide, four-high clear passageways.
for(let z=-3;z<=1;z++)for(let x=-39;x<=-33;x++)for(let y of [95,96,97,98])checks.push({x:-346+x,y,z:2778+z,block:'minecraft:air'});
for(let x=-3;x<=1;x++)for(let z=33;z<=39;z++)for(let y of [95,96,97,98])checks.push({x:-346+x,y,z:2778+z,block:'minecraft:air'});
// Southeast axis has a full-height route through the oblique arch.
const n=Math.hypot(31.5,27.5);for(let u=33;u<=39;u++)for(let y of [95,96,97,98])checks.push({x:-346+Math.round(u*31.5/n),y,z:2778+Math.round(u*27.5/n),block:'minecraft:air'});
const free=await read(checks),obstructed=free.filter(p=>!matches(p.actual,p.block));
// Stairs require two blocks of headroom above each half-step.
const stairChecks=[];
for(let i=0;i<16;i++)for(let x=29;x<=31;x++)for(let h=1;h<=2;h++)stairChecks.push({x:-346+x,y:95+Math.floor(i/2)+h,z:2778-9+i,block:'minecraft:air'});
const stairs=await read(stairChecks),stairFailed=stairs.filter(p=>!matches(p.actual,p.block));
const report={placed:results.length,mismatches:failed,clearanceChecks:free.length,obstructions:obstructed,stairHeadroomChecks:stairs.length,stairHeadroomFailures:stairFailed};
fs.writeFileSync(dir+'verification.json',JSON.stringify(report,null,2));
console.log(JSON.stringify(report));
if(failed.length||obstructed.length||stairFailed.length)process.exitCode=1;
