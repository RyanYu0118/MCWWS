import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/island-castle-refinement-20261020/';
const key=p=>`${p.x},${p.y},${p.z}`;
const old=new Map(JSON.parse(fs.readFileSync(dir+'plan-before-glazing.json')).blocks.map(p=>[key(p),p]));
const final=JSON.parse(fs.readFileSync(dir+'plan.json'));
const patch=final.blocks.filter(p=>old.get(key(p))?.block!==p.block);
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(e,body){const r=await fetch('http://127.0.0.1:8765/'+e,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...body})});const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));return j;}
async function read(list){let i=0;const out=[];await Promise.all(Array.from({length:24},async()=>{while(i<list.length){const p=list[i++];out.push({...p,actual:(await api('get_block',{x:p.x,y:p.y,z:p.z})).block});}}));return out;}
const before=await read(patch);
if(before.some(p=>p.actual.split('[')[0]!==old.get(key(p)).block.split('[')[0]))throw Error('Concurrent edit');
fs.writeFileSync(dir+'glazing-before.json',JSON.stringify(before));
console.log('Glazing refinement',patch.length,await api('set_blocks',{blocks:patch}));
const after=await read(patch),failures=after.filter(p=>p.actual!==p.block);
fs.writeFileSync(dir+'glazing-verification.json',JSON.stringify({checked:patch.length,failures},null,2));
console.log(JSON.stringify({checked:patch.length,failures:failures.length}));
if(failures.length)process.exitCode=1;
