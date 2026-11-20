import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/ocean-restoration-20261105/';
const blocks=JSON.parse(fs.readFileSync(dir+'ice-edge-blocks.json'));
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(e,b){const r=await fetch('http://127.0.0.1:8765/'+e,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...b})});const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));return j;}
async function read(points){let i=0,out=[];await Promise.all(Array.from({length:24},async()=>{while(i<points.length){const n=i++,p=points[n];out[n]={...p,actual:(await api('get_block',{x:p.x,y:p.y,z:p.z})).block};}}));return out;}
const before=await read(blocks);fs.writeFileSync(dir+'ice-edge-before.json',JSON.stringify(before));
const bad=before.filter(p=>!['minecraft:water','minecraft:ice'].includes(p.actual.split('[')[0]));
if(bad.length)throw Error('Unexpected ice-edge source '+JSON.stringify(bad.slice(0,5)));
const ps=(await api('list_players',{})).players;
if(blocks.some(b=>ps.some(p=>p.world==='world'&&Math.abs(b.x-p.x)<=6&&Math.abs(b.y-p.y)<=6&&Math.abs(b.z-p.z)<=6)))throw Error('Player near ice edge');
const result=await api('set_blocks',{blocks});fs.writeFileSync(dir+'ice-edge-write.json',JSON.stringify(result));console.log(JSON.stringify(result));
const samples=blocks.filter((_,i)=>i%53===0),after=await read(samples),failures=after.filter(p=>p.actual.split('[')[0]!==p.block);
fs.writeFileSync(dir+'ice-edge-verification.json',JSON.stringify({samples:samples.length,failures}));console.log('ice-edge verification',samples.length,'failures',failures.length);
if(failures.length)process.exitCode=1;
