import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/ocean-restoration-20261105/';
const plan=JSON.parse(fs.readFileSync(dir+'write-plan.json'));
const audit=JSON.parse(fs.readFileSync(dir+(process.argv[2]==='verify'?'audit-samples-final.json':'audit-samples.json')));
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(endpoint,body){const r=await fetch('http://127.0.0.1:8765/'+endpoint,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...body}),signal:AbortSignal.timeout(180000)});const j=await r.json();if(!r.ok||!j.ok)throw Error(endpoint+': '+JSON.stringify(j));return j;}
async function read(points){let i=0;const out=new Array(points.length);await Promise.all(Array.from({length:20},async()=>{while(i<points.length){const n=i++,p=points[n];out[n]={...p,actual:(await api('get_block',{x:p.x,y:p.y,z:p.z})).block};}}));return out;}
const id=s=>s.split('[')[0];
const mode=process.argv[2]??'preflight';
if(mode==='preflight'){
 const changeReads=await read(audit.changes),protectedReads=await read(audit.protected);
 const mismatches=changeReads.filter(p=>id(p.actual)!==p.before);
 const protectedMismatches=protectedReads.filter(p=>id(p.actual)!==p.block);
 fs.writeFileSync(dir+'live-preflight.json',JSON.stringify({time:new Date().toISOString(),changeReads,protectedReads,mismatches,protectedMismatches}));
 console.log(JSON.stringify({checked:changeReads.length,protected:protectedReads.length,mismatches:mismatches.length,protectedMismatches:protectedMismatches.length}));
 if(mismatches.length||protectedMismatches.length){console.log(JSON.stringify({mismatches:mismatches.slice(0,8),protectedMismatches:protectedMismatches.slice(0,8)}));process.exitCode=1;}
}else if(mode==='apply'){
 const pre=JSON.parse(fs.readFileSync(dir+'live-preflight.json'));
 if(pre.mismatches.length||pre.protectedMismatches.length)throw Error('Preflight failed');
 const journal=dir+'writes.jsonl';const done=fs.existsSync(journal)?new Set(fs.readFileSync(journal,'utf8').trim().split('\n').filter(Boolean).map(s=>JSON.parse(s).batch)):new Set();
 let changed=0;
 for(let n=0;n<plan.batches;n++){
  if(done.has(n))continue;
  const body=JSON.parse(fs.readFileSync(dir+`jobs-${String(n).padStart(3,'0')}.json`));
  const result=await api('fawe_bulk',body);
  fs.appendFileSync(journal,JSON.stringify({batch:n,time:new Date().toISOString(),result})+'\n');
  changed+=result.changed;console.log('batch',n+1,'/',plan.batches,'changed',result.changed,'session total',changed);
 }
}else if(mode==='verify'){
 const after=await read(audit.changes),protectedAfter=await read(audit.protected);
 const finishingAfter=await read(JSON.parse(fs.readFileSync(dir+'finishing-audit.json')));
 const finishingFailures=finishingAfter.filter(p=>id(p.actual)!==p.block);
 const failures=after.filter(p=>id(p.actual)!==p.after);
 const protectedFailures=protectedAfter.filter(p=>id(p.actual)!==p.block);
 const liveBefore=JSON.parse(fs.readFileSync(dir+'live-preflight.json'));
 const stateChanges=protectedAfter.filter((p,i)=>p.actual!==liveBefore.protectedReads[i].actual);
 const report={time:new Date().toISOString(),plannedChanges:plan.changed_blocks,changedSamples:after.length,finishingSamples:finishingAfter.length,protectedSamples:protectedAfter.length,failures,finishingFailures,protectedFailures,protectedStateChanges:stateChanges};
 fs.writeFileSync(dir+'verification.json',JSON.stringify(report,null,2));
 console.log(JSON.stringify(report));
 if(failures.length||finishingFailures.length||protectedFailures.length||stateChanges.length)process.exitCode=1;
}else throw Error('Unknown mode');
