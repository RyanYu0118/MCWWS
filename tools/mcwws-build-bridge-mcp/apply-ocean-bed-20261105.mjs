import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/ocean-bed-refinement-20261105/';
const mode=process.argv[2]??'preflight',plan=JSON.parse(fs.readFileSync(dir+'write-plan.json')),audit=JSON.parse(fs.readFileSync(dir+'audit.json'));
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(e,b){const r=await fetch('http://127.0.0.1:8765/'+e,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...b}),signal:AbortSignal.timeout(120000)});const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));return j;}
const id=s=>s.split('[')[0];
async function read(points){let i=0;const out=new Array(points.length);await Promise.all(Array.from({length:24},async()=>{while(i<points.length){const n=i++,p=points[n];out[n]={...p,actual:(await api('get_block',{x:p.x,y:p.y,z:p.z})).block};}}));return out;}
if(mode==='preflight'){
 const changes=await read(audit.changes),protectedPoints=await read([...audit.structures,...audit.reference,...audit.surface]);
 const mismatches=changes.filter(p=>id(p.actual)!==p.before),protectedMismatches=protectedPoints.filter(p=>id(p.actual)!==p.block);
 fs.writeFileSync(dir+'preflight.json',JSON.stringify({time:new Date().toISOString(),changes,protectedPoints,mismatches,protectedMismatches}));
 console.log(JSON.stringify({changedSamples:changes.length,protectedSamples:protectedPoints.length,mismatches:mismatches.length,protectedMismatches:protectedMismatches.length,examples:mismatches.slice(0,6)}));
 if(mismatches.length||protectedMismatches.length)process.exitCode=1;
}else if(mode==='apply'){
 const pre=JSON.parse(fs.readFileSync(dir+'preflight.json'));if(pre.mismatches.length||pre.protectedMismatches.length)throw Error('Preflight failed');
 const names=JSON.parse(fs.readFileSync(dir+'target-palette.json')),data=fs.readFileSync(dir+'placements.u16'),count=data.length/8;
 const journal=dir+'writes.jsonl',done=fs.existsSync(journal)?new Set(fs.readFileSync(journal,'utf8').trim().split('\n').filter(Boolean).map(s=>JSON.parse(s).batch)):new Set();
 let total=0;
 for(let offset=0,batch=0;offset<count;offset+=20000,batch++){
  if(done.has(batch))continue;
  const blocks=[],deferred=[],players=(await api('list_players',{})).players??[];
  for(let n=offset;n<Math.min(count,offset+20000);n++){
   const b={x:data.readUInt16LE(n*8)-1200,y:data.readUInt16LE(n*8+2)-64,z:data.readUInt16LE(n*8+4)+2800,block:names[data.readUInt16LE(n*8+6)]};
   (players.some(p=>p.world==='world'&&Math.abs(b.x-p.x)<=6&&Math.abs(b.y-p.y)<=6&&Math.abs(b.z-p.z)<=6)?deferred:blocks).push(b);
  }
  if(deferred.length)fs.appendFileSync(dir+'deferred.jsonl',JSON.stringify(deferred)+'\n');
  const result=blocks.length?await api('set_blocks',{blocks}):{ok:true,changed:0};total+=result.changed;
  fs.appendFileSync(journal,JSON.stringify({batch,time:new Date().toISOString(),result})+'\n');
  if(batch%15===0||offset+20000>=count)console.log('Written',total,'/',count,'batch',batch+1);
 }
 console.log('APPLY COMPLETE',total);
}else if(mode==='verify'){
 const changes=await read(audit.changes),protectedPoints=await read([...audit.structures,...audit.reference,...audit.surface]);
 const profiles=await read(JSON.parse(fs.readFileSync(dir+'profile-audit.json')));
 const pre=JSON.parse(fs.readFileSync(dir+'preflight.json'));
 const failures=changes.filter(p=>id(p.actual)!==p.after),protectedFailures=protectedPoints.filter((p,i)=>p.actual!==pre.protectedPoints[i].actual);
 const profileFailures=profiles.filter(p=>id(p.actual)!==p.block);
 const report={time:new Date().toISOString(),changedSamples:changes.length,protectedSamples:protectedPoints.length,profileSamples:profiles.length,failures,protectedFailures,profileFailures};fs.writeFileSync(dir+'verification.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report));
 if(failures.length||protectedFailures.length||profileFailures.length)process.exitCode=1;
}else throw Error('Unknown mode');
