import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/island-magic-links-20261020/';
const plan=JSON.parse(fs.readFileSync(dir+'plan.json'));
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(e,body){
 const r=await fetch('http://127.0.0.1:8765/'+e,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...body})});
 const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));return j;
}
async function read(list){
 let i=0;const result=new Array(list.length);
 await Promise.all(Array.from({length:24},async()=>{while(i<list.length){const n=i++,p=list[n];result[n]={x:p.x,y:p.y,z:p.z,block:(await api('get_block',{x:p.x,y:p.y,z:p.z})).block};}}));return result;
}
const seen=new Set();
for(const p of plan.blocks){
 if(p.x< -388||p.x> -303||p.y<143||p.y>221||p.z<2738||p.z>2819)throw Error('Outside approved bounds');
 const k=`${p.x},${p.y},${p.z}`;if(seen.has(k))throw Error('Duplicate coordinate');seen.add(k);
}
const contactPoints=plan.streams.flatMap(s=>[s.tower,s.anchor]).map(([x,y,z])=>({x,y,z}));
const before=await read(plan.blocks),contactsBefore=await read(contactPoints);
const conflicts=before.filter(p=>p.block!=='minecraft:air');
if(conflicts.length){fs.writeFileSync(dir+'conflicts.json',JSON.stringify(conflicts,null,2));throw Error('Occupied build positions: '+conflicts.length);}
for(let i=0;i<plan.streams.length;i++){
 if(!contactsBefore[i*2].block.startsWith('minecraft:iron_bars'))throw Error('Tower finial changed');
 if(contactsBefore[i*2+1].block==='minecraft:air')throw Error('Island contact missing');
}
fs.writeFileSync(dir+'before-'+Date.now()+'.json',JSON.stringify({blocks:before,contacts:contactsBefore}));
console.log('Preflight passed:',before.length,'air positions; all eight contact points present.');
const result=await api('set_blocks',{blocks:plan.blocks});
fs.appendFileSync(dir+'writes.jsonl',JSON.stringify({time:new Date().toISOString(),result})+'\n');
console.log('Placed',JSON.stringify(result));
const after=await read(plan.blocks),contactsAfter=await read(contactPoints);
const failures=after.filter((p,i)=>p.block.split('[')[0]!==plan.blocks[i].block);
const changedContacts=contactsAfter.filter((p,i)=>p.block!==contactsBefore[i].block);
for(const s of plan.streams){
 for(const [x,y,z]of [[s.tower[0],s.tower[1]+1,s.tower[2]],[s.anchor[0],s.anchor[1]-1,s.anchor[2]]]){
  const p=after.find(v=>v.x===x&&v.y===y&&v.z===z);
  if(!p||p.block.split('[')[0]!=='minecraft:pearlescent_froglight')failures.push({x,y,z,reason:'Missing contact crystal'});
 }
}
const report={time:new Date().toISOString(),checked:after.length,streams:plan.streams.length,failures,changedContacts};
fs.writeFileSync(dir+'verification.json',JSON.stringify(report,null,2));
console.log(JSON.stringify(report));
if(failures.length||changedContacts.length)process.exitCode=1;
