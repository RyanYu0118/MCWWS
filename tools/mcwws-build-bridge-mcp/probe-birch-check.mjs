import fs from "fs";
const token = fs.readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml","utf8").match(/token:\s*(\S+)/)[1];
async function get(x,y,z){
  const r=await fetch("http://127.0.0.1:8765/get_block",{method:"POST",headers:{Authorization:`Bearer ${token}`,"Content-Type":"application/json"},body:JSON.stringify({world:"world",x,y,z})});
  return ((await r.json()).block||"").replace(/^minecraft:/,"").split("[")[0];
}
let logs=0, leaves=0, minY=999, maxY=0;
for (let y=64; y<=100; y++){
  const b=await get(-624,y,418);
  if (b==="birch_log"){logs++; minY=Math.min(minY,y); maxY=Math.max(maxY,y);} 
}
console.log("trunk col", logs, minY, maxY, "top", await get(-624,maxY+1,418), await get(-623,86,418));
// canopy sample
for (const [x,y,z] of [[-624,86,418],[-620,82,418],[-624,90,418],[-617,80,418]]) {
  console.log(x,y,z, await get(x,y,z));
}
console.log("ground around trunk y63");
for (let z=415; z<=422; z++){
  let row="";
  for (let x=-628; x<=-619; x++){
    const g=await get(x,63,z);
    const a=await get(x,64,z);
    row += a==="birch_log"?"T": g==="grass_block"?"g": g[0];
  }
  console.log(z,row);
}
