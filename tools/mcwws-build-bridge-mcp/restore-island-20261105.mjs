import fs from 'node:fs';
const dir='tools/mcwws-build-bridge-mcp/island-repair-20261105/',source='tools/mcwws-build-bridge-mcp/purple-coast-expansion-20261105/';
const mode=process.argv[2]??'preflight',audit=JSON.parse(fs.readFileSync(dir+'audit.json')),plan=JSON.parse(fs.readFileSync(dir+'plan.json'));
const token=fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml','utf8').match(/token:\s*(\S+)/)[1];
async function api(e,b){const r=await fetch('http://127.0.0.1:8765/'+e,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({world:'world',...b}),signal:AbortSignal.timeout(120000)});const j=await r.json();if(!r.ok||!j.ok)throw Error(JSON.stringify(j));return j;}
const id=s=>s.split('[')[0];
function canonical(s){const [n,p]=s.replace(']','').split('[');return n+'['+(p?p.split(',').sort().join(','):'')+']';}
async function read(points){let i=0,out=new Array(points.length);await Promise.all(Array.from({length:24},async()=>{while(i<points.length){const n=i++,p=points[n];out[n]={...p,actual:(await api('get_block',{x:p.x,y:p.y,z:p.z})).block};}}));return out;}
if(mode==='preflight'){
 const changes=await read(audit.changes),protectedPoints=await read(audit.protected);
 const mismatches=changes.filter(p=>id(p.actual)!==p.before&&id(p.actual)!==id(p.after)&&!['minecraft:air','minecraft:cave_air','minecraft:void_air'].includes(id(p.actual)));
 const protectedMismatches=protectedPoints.filter(p=>id(p.actual)!==id(p.block));
 fs.writeFileSync(dir+'preflight.json',JSON.stringify({time:new Date().toISOString(),changes,protectedPoints,mismatches,protectedMismatches}));console.log(JSON.stringify({changes:changes.length,protected:protectedPoints.length,mismatches:mismatches.length,protectedMismatches:protectedMismatches.length,examples:[...mismatches,...protectedMismatches].slice(0,5)}));
 if(mismatches.length||protectedMismatches.length)process.exitCode=1;
}else if(mode==='apply'){
 const pre=JSON.parse(fs.readFileSync(dir+'preflight.json'));if(pre.mismatches.length||pre.protectedMismatches.length)throw Error('Preflight failed');
 const states=JSON.parse(fs.readFileSync(dir+'states.json')),data=fs.readFileSync(dir+'placements.u16'),count=data.length/8;
 const journal=dir+'writes.jsonl',done=fs.existsSync(journal)?new Set(fs.readFileSync(journal,'utf8').trim().split('\n').filter(Boolean).map(s=>JSON.parse(s).batch)):new Set();let total=0;
 const passable=new Set(['minecraft:air','minecraft:cave_air','minecraft:void_air','minecraft:water','minecraft:fern','minecraft:large_fern','minecraft:short_grass','minecraft:tall_grass','minecraft:seagrass','minecraft:tall_seagrass','minecraft:kelp','minecraft:kelp_plant','minecraft:vine','minecraft:glow_lichen','minecraft:hanging_roots']);
 for(let offset=0,batch=0;offset<count;offset+=20000,batch++){
  if(done.has(batch))continue;
  const players=(await api('list_players',{})).players??[],blocks=[],deferred=[];
  for(let n=offset;n<Math.min(count,offset+20000);n++){
   const b={x:data.readUInt16LE(n*8)-920,y:data.readUInt16LE(n*8+2)-64,z:data.readUInt16LE(n*8+4)+2940,block:states[data.readUInt16LE(n*8+6)]};
   const collision=!passable.has(id(b.block))&&players.some(p=>p.world==='world'&&b.x<p.x+.3&&b.x+1>p.x-.3&&b.z<p.z+.3&&b.z+1>p.z-.3&&b.y<p.y+1.8&&b.y+1>p.y+.03);
   (collision?deferred:blocks).push(b);
  }
  if(deferred.length)fs.appendFileSync(dir+'deferred.jsonl',JSON.stringify(deferred)+'\n');
  const result=blocks.length?await api('set_blocks',{blocks}):{ok:true,changed:0};total+=result.changed;fs.appendFileSync(journal,JSON.stringify({batch,time:new Date().toISOString(),result})+'\n');
  if(batch%5===0||offset+20000>=count)console.log('Restored',total,'/',count,'batch',batch+1);
 }
 console.log('RESTORE COMPLETE',total);
}else if(mode==='verify'){
 const changes=await read(audit.changes),protectedPoints=await read(audit.protected),tops=await read(audit.tops),pre=JSON.parse(fs.readFileSync(dir+'preflight.json'));
 const failures=changes.filter(p=>canonical(p.actual)!==canonical(p.after)),protectedFailures=protectedPoints.filter((p,i)=>p.actual!==pre.protectedPoints[i].actual),topFailures=tops.filter(p=>canonical(p.actual)!==canonical(p.block));
 const report={time:new Date().toISOString(),restorationSamples:changes.length,protectedSamples:protectedPoints.length,surfaceSamples:tops.length,failures,protectedFailures,topFailures};fs.writeFileSync(dir+'verification.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report));if(failures.length||protectedFailures.length||topFailures.length)process.exitCode=1;
}else throw Error('Unknown mode');
