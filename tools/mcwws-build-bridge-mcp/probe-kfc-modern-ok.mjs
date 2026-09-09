import fs from "fs";
const token = fs.readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml","utf8").match(/token:\s*(\S+)/)[1];
async function get(x,y,z){const r=await fetch("http://127.0.0.1:8765/get_block",{method:"POST",headers:{Authorization:"Bearer "+token,"Content-Type":"application/json"},body:JSON.stringify({world:"world",x,y,z})});return ((await r.json()).block||"").replace("minecraft:","").split("[")[0];}
console.log("door", await get(-542,64,210), await get(-542,65,210));
console.log("wing south glass", await get(-552,65,212), await get(-552,66,212));
console.log("bay vs wing", await get(-542,64,210), await get(-552,64,212));
console.log("patio", await get(-542,63,208), await get(-545,64,208));
console.log("cantilever", await get(-542,68,209), await get(-542,69,209));
console.log("sign sample", await get(-545,70,210), await get(-542,72,210));
console.log("road untouched", await get(-542,64,202), await get(-542,64,199));
