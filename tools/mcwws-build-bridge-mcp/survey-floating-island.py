import io,zlib,json,collections
from pathlib import Path
import nbtlib
root=Path('world/dimensions/minecraft/overworld/region')
output=Path('tools/mcwws-build-bridge-mcp/island-magic-links-20261020')
output.mkdir(exist_ok=True)
x1,x2,z1,z2=-406,-286,2718,2838
regions={};columns={};hist=collections.Counter();chunks=0
air={'minecraft:air','minecraft:cave_air','minecraft:void_air'}
for cx in range(x1//16,x2//16+1):
 for cz in range(z1//16,z2//16+1):
  reg=(cx//32,cz//32)
  if reg not in regions:
   f=root/f'r.{reg[0]}.{reg[1]}.mca'
   regions[reg]=f.read_bytes() if f.exists() else b''
  raw=regions[reg]
  if not raw:continue
  i=cx%32+(cz%32)*32;v=int.from_bytes(raw[i*4:i*4+4],'big');off=(v>>8)*4096
  if not off:continue
  length=int.from_bytes(raw[off:off+4],'big');kind=raw[off+4]
  if kind!=2:raise RuntimeError(f'Unsupported compression {kind}')
  chunk=nbtlib.File.parse(io.BytesIO(zlib.decompress(raw[off+5:off+4+length])))
  chunks+=1
  for section in chunk['sections']:
   sy=int(section['Y']);states=section.get('block_states')
   if sy<9 or sy>19 or not states:continue
   palette=[str(v['Name']) for v in states['palette']]
   if all(v in air for v in palette):continue
   bits=max(4,(len(palette)-1).bit_length());per=64//bits;mask=(1<<bits)-1
   data=[int(v)&((1<<64)-1) for v in states.get('data',[])]
   for idx in range(4096):
    name=palette[(data[idx//per]>>((idx%per)*bits))&mask] if data else palette[0]
    if name in air:continue
    x=cx*16+(idx&15);z=cz*16+((idx>>4)&15);y=sy*16+(idx>>8)
    if x<x1 or x>x2 or z<z1 or z>z2 or y<145:continue
    key=(x,z);col=columns.setdefault(key,{'x':x,'z':z,'min':y,'max':y,'bottom':name,'count':0})
    if y<col['min']:col.update(min=y,bottom=name)
    col['max']=max(col['max'],y);col['count']+=1;hist[y]+=1
result={'bounds':[x1,x2,z1,z2],'columns':list(columns.values()),'layers':dict(hist),'chunks':chunks}
(output/'island-survey.json').write_text(json.dumps(result))
print('Chunks',chunks,'occupied columns',len(columns),'overall height',min(hist),max(hist))
print('Layers',sorted(hist.items()))
for dx,dz in [(36,13),(-13,36),(-36,-13),(13,-36)]:
 x,z=-346+dx,2778+dz
 near=sorted(columns.values(),key=lambda c:(c['x']-x)**2+(c['z']-z)**2)[:6]
 print('Tower',x,z,'nearest island bottoms',near)
