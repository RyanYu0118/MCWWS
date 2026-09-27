import fs from "fs";
const token = fs.readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml","utf8").match(/token:\s*(\S+)/)[1];
async function get(x,y,z){
  const r=await fetch("http://127.0.0.1:8765/get_block",{method:"POST",headers:{Authorization:`Bearer ${token}`,"Content-Type":"application/json"},body:JSON.stringify({world:"world",x,y,z})});
  return ((await r.json()).block||"").replace(/^minecraft:/,"").split("[")[0];
}
const cx=-5028, cz=1507;
const pass = new Set(["air","short_grass","tall_grass","grass_block","dirt","fern","large_fern","poppy","dandelion","oxeye_daisy","cornflower","azure_bluet","allium","lilac","peony","rose_bush","oak_sapling","birch_sapling","vine"]);
console.log("y63 map x-5040..-5016 z1495..1519  (player ~ -5028,1507)");
for (let z=cz-8; z<=cz+8; z++){
  let row="";
  for (let x=cx-8; x<=cx+8; x++){
    const g=await get(x,63,z);
    const a=await get(x,64,z);
    if (g==="grass_block" && (a==="air"||a==="short_grass"||a==="tall_grass"||a==="fern")) row+="g";
    else if (!pass.has(g) || (a!=="air" && !pass.has(a))) row+="X";
    else row+=g[0];
  }
  console.log(String(z).padStart(5), row);
}
let bad=[];
for (let y=64; y<=90; y+=2){
  for (let z=cz-6; z<=cz+6; z+=2){
    for (let x=cx-6; x<=cx+6; x+=2){
      const b=await get(x,y,z);
      if (!pass.has(b)) bad.push(`${x},${y},${z}:${b}`);
    }
  }
}
console.log("solids", bad.slice(0,40).join(" | ") || "none", "count", bad.length);
