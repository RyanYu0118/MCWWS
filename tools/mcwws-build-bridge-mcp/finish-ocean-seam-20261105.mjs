import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/ocean-restoration-20261105/';
const blocks=JSON.parse(fs.readFileSync(dir+'seam-blocks.json'));
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(e,b){const r=await fetch('http://127.0.0.1:8765/'+e,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...b})});const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));return j;}
async function read(points){let i=0,out=[];await Promise.all(Array.from({length:24},async()=>{while(i<points.length){const n=i++,p=points[n];out[n]={...p,actual:(await api('get_block',{x:p.x,y:p.y,z:p.z})).block};}}));return out;}
const sample=blocks.filter((_,i)=>i%83===0),before=await read(sample);fs.writeFileSync(dir+'seam-preflight.json',JSON.stringify(before));
if(before.some(b=>b.actual.split('[')[0]!==b.before))throw Error('Seam preflight mismatch');
const ps=(await api('list_players',{})).players;if(blocks.some(b=>ps.some(p=>p.world==='world'&&Math.abs(b.x-p.x)<=6&&Math.abs(b.y-p.y)<=6&&Math.abs(b.z-p.z)<=6)))throw Error('Player near seam');
for(let i=0;i<blocks.length;i+=20000){const r=await api('set_blocks',{blocks:blocks.slice(i,i+20000).map(({x,y,z,block})=>({x,y,z,block}))});fs.appendFileSync(dir+'seam-writes.jsonl',JSON.stringify(r)+'\n');console.log(JSON.stringify(r));}
const after=await read(sample),failures=after.filter(b=>b.actual.split('[')[0]!==b.block);fs.writeFileSync(dir+'seam-verification.json',JSON.stringify({samples:after.length,failures}));console.log('Seam samples',after.length,'failures',failures.length);if(failures.length)process.exitCode=1;
