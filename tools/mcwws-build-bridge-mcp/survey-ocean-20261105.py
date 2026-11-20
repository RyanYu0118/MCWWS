import io,json,zlib,time,collections
from pathlib import Path
import numpy as np
import nbtlib
from PIL import Image,ImageDraw

OUT=Path('tools/mcwws-build-bridge-mcp/ocean-restoration-20261105');OUT.mkdir(exist_ok=True)
X0,X1,Z0,Z1,Y0,Y1=-1200,-440,2800,3500,0,191
grid=np.zeros((Z1-Z0+1,Y1-Y0+1,X1-X0+1),dtype=np.uint16)
names=['minecraft:air'];ids={names[0]:0};regs={};count=0
for cz in range(Z0//16,Z1//16+1):
 for cx in range(X0//16,X1//16+1):
  rk=(cx//32,cz//32)
  if rk not in regs:
   p=Path('world/dimensions/minecraft/overworld/region')/f'r.{rk[0]}.{rk[1]}.mca';regs[rk]=p.read_bytes() if p.exists() else b''
  raw=regs[rk]
  if not raw:continue
  ix=(cx%32+cz%32*32)*4;off=(int.from_bytes(raw[ix:ix+4],'big')>>8)*4096
  if not off:continue
  length=int.from_bytes(raw[off:off+4],'big')
  if raw[off+4]!=2:raise RuntimeError('Compression')
  chunk=nbtlib.File.parse(io.BytesIO(zlib.decompress(raw[off+5:off+4+length])))
  lx0=max(X0-cx*16,0);lx1=min(X1-cx*16+1,16);lz0=max(Z0-cz*16,0);lz1=min(Z1-cz*16+1,16)
  for sec in chunk['sections']:
   sy=int(sec['Y']);st=sec.get('block_states')
   if sy<0 or sy>11 or not st:continue
   pal=[]
   for v in st['palette']:
    n=str(v['Name'])
    if n not in ids:ids[n]=len(names);names.append(n)
    pal.append(ids[n])
   if len(pal)==1:a=np.full((16,16,16),pal[0],dtype=np.uint16)
   else:
    b=max(4,(len(pal)-1).bit_length());per=64//b;d=np.asarray(st['data'],dtype=np.int64).view(np.uint64);ind=np.arange(4096,dtype=np.uint64)
    pi=((d[ind//per]>>((ind%per)*b))&((1<<b)-1)).astype(np.int32)
    a=np.asarray(pal,dtype=np.uint16)[pi].reshape(16,16,16)
   grid[cz*16+lz0-Z0:cz*16+lz1-Z0,sy*16:sy*16+16,cx*16+lx0-X0:cx*16+lx1-X0]=a.transpose(1,0,2)[lz0:lz1,:,lx0:lx1]
  count+=1
 if cz%8==0:print('survey chunks',count,flush=True)
np.save(OUT/'grid-before.npy',grid)
(OUT/'palette.json').write_text(json.dumps({'names':names,'bounds':[X0,X1,Y0,Y1,Z0,Z1]}))
air=np.isin(grid,[ids.get(n,-1) for n in ['minecraft:air','minecraft:cave_air','minecraft:void_air']]);water=grid==ids.get('minecraft:water',-1)
top=np.where(~air,np.arange(192)[None,:,None],-1).max(axis=1)
topwater=np.where(water,np.arange(192)[None,:,None],-1).max(axis=1)
np.savez_compressed(OUT/'surface.npz',top=top,water=topwater)
# Internal survey map, north at top; grayscale height, blue water, orange built materials.
natural={'air','cave_air','void_air','water','lava','stone','deepslate','granite','diorite','andesite','tuff','calcite','dripstone_block','pointed_dripstone','dirt','coarse_dirt','rooted_dirt','grass_block','podzol','mycelium','sand','red_sand','sandstone','red_sandstone','gravel','clay','mud','packed_mud','snow','snow_block','ice','packed_ice','blue_ice','bedrock','moss_block','moss_carpet','short_grass','tall_grass','fern','large_fern','seagrass','tall_seagrass','kelp','kelp_plant','vine','glow_lichen','cave_vines','cave_vines_plant','hanging_roots','brown_mushroom','red_mushroom','dead_bush','lily_pad','sugar_cane','pumpkin','melon','sweet_berry_bush','bamboo','bamboo_sapling'}
def isnatural(n):
 s=n.removeprefix('minecraft:')
 return s in natural or s.endswith(('_ore','_leaves','_log','_wood','_sapling','_flower','_tulip','_roots')) or s in ['dandelion','poppy','allium','azure_bluet','oxeye_daisy','cornflower','lilac','rose_bush','peony','sunflower']
crafted=np.array([not isnatural(n) for n in names]);craft=crafted[grid]
craftcols=craft.any(axis=1);np.save(OUT/'craft-columns.npy',craftcols)
g=np.clip(top*1.2+35,35,245).astype(np.uint8);rgb=np.stack([g,g,g],axis=2)
rgb[topwater>=top]=[30,85,150];rgb[craftcols]=[235,145,30]
im=Image.fromarray(rgb).resize((1522,1402));draw=ImageDraw.Draw(im)
for x in range(-1200,-439,100):
 px=(x-X0)*2;draw.line((px,0,px,1401),fill=(90,90,100));draw.text((px+2,5),str(x),fill='white')
for z in range(2800,3501,100):
 pz=(z-Z0)*2;draw.line((0,pz,1521,pz),fill=(90,90,100));draw.text((3,pz+3),str(z),fill='white')
draw.ellipse((( -639-X0)*2-7,(3149-Z0)*2-7,(-639-X0)*2+7,(3149-Z0)*2+7),outline='red',width=3)
im.save(OUT/'survey.png')
print('DONE',count,'chunks',len(names),'materials','crafted columns',int(craftcols.sum()),flush=True)
