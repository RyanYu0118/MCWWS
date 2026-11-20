import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/ocean-restoration-20261105/';
const plan=JSON.parse(fs.readFileSync(dir+'write-plan.json'));
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function request(endpoint,body){const r=await fetch('http://127.0.0.1:8765/'+endpoint,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...body}),signal:AbortSignal.timeout(120000)});const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));return j;}
async function api(body){const players=(await request('list_players',{})).players??[];const allowed=[],deferred=[];for(const b of body)(players.some(p=>p.world==='world'&&Math.abs(b.x-p.x)<=6&&Math.abs(b.y-p.y)<=6&&Math.abs(b.z-p.z)<=6)?deferred:allowed).push(b);if(deferred.length)fs.appendFileSync(dir+'deferred.jsonl',JSON.stringify(deferred)+'\n');return allowed.length?request('set_blocks',{blocks:allowed}):{ok:true,changed:0};}
const journal=dir+'writes-direct.jsonl';
const done=fs.existsSync(journal)?new Set(fs.readFileSync(journal,'utf8').trim().split('\n').filter(Boolean).map(s=>JSON.parse(s).batch)):new Set();
let batch=0,blocks=[],total=0;
async function flush(){if(!blocks.length)return;const n=batch++;if(!done.has(n)){const start=Date.now(),result=await api(blocks);fs.appendFileSync(journal,JSON.stringify({batch:n,time:new Date().toISOString(),result})+'\n');total+=result.changed;console.log('direct batch',n+1,'blocks',result.changed,'ms',Date.now()-start,'session total',total);}blocks=[];}
for(let n=2;n<plan.batches;n++){
 const path=dir+`jobs-${String(n).padStart(3,'0')}${n===2?'.saved':''}.json`;
 for(const a of JSON.parse(fs.readFileSync(path)).jobs){
  for(let x=a.x1;x<=a.x2;x++)for(let z=a.z1;z<=a.z2;z++)for(let y=a.y1;y<=a.y2;y++){
   blocks.push({x,y,z,block:a.block});if(blocks.length>=20000)await flush();
  }
 }
}
await flush();
for(const b of JSON.parse(fs.readFileSync(dir+'finishing-blocks.json'))){blocks.push(b);if(blocks.length>=20000)await flush();}
await flush();console.log('DIRECT COMPLETE',total);
