import io,json,zlib,collections
from pathlib import Path
import numpy as np
import nbtlib
from PIL import Image,ImageDraw
P=Path('tools/mcwws-build-bridge-mcp/island-east-blend-20261105');P.mkdir(exist_ok=True)
X0,X1,Z0,Z1,Y0,Y1=-650,-455,2970,3155,-64,319
G=np.zeros((Z1-Z0+1,Y1-Y0+1,X1-X0+1),np.uint16);names=['minecraft:air'];ids={names[0]:0};regs={};chunks=0
for cz in range(Z0//16,Z1//16+1):
 for cx in range(X0//16,X1//16+1):
  rk=(cx//32,cz//32)
  if rk not in regs:
   f=Path('world/dimensions/minecraft/overworld/region')/f'r.{rk[0]}.{rk[1]}.mca';regs[rk]=f.read_bytes() if f.exists() else b''
  raw=regs[rk]
  if not raw:continue
  ix=(cx%32+cz%32*32)*4;off=(int.from_bytes(raw[ix:ix+4],'big')>>8)*4096
  if not off:continue
  n=int.from_bytes(raw[off:off+4],'big')
  if raw[off+4]!=2:raise RuntimeError('Unexpected compression')
  c=nbtlib.File.parse(io.BytesIO(zlib.decompress(raw[off+5:off+4+n])))
  xa=max(X0-cx*16,0);xb=min(X1-cx*16+1,16);za=max(Z0-cz*16,0);zb=min(Z1-cz*16+1,16)
  for sec in c['sections']:
   sy=int(sec['Y']);st=sec.get('block_states')
   if sy*16<Y0 or sy*16>Y1 or not st:continue
   pal=[]
   for v in st['palette']:
    s=str(v['Name'])
    if s not in ids:ids[s]=len(names);names.append(s)
    pal.append(ids[s])
   if len(pal)==1:a=np.full((16,16,16),pal[0],np.uint16)
   else:
    b=max(4,(len(pal)-1).bit_length());per=64//b;d=np.asarray(st['data'],dtype=np.int64).view(np.uint64);ind=np.arange(4096,dtype=np.uint64)
    val=((d[ind//per]>>((ind%per)*b))&((1<<b)-1)).astype(np.int32);a=np.asarray(pal,np.uint16)[val].reshape(16,16,16)
   G[cz*16+za-Z0:cz*16+zb-Z0,sy*16-Y0:sy*16+16-Y0,cx*16+xa-X0:cx*16+xb-X0]=a.transpose(1,0,2)[za:zb,:,xa:xb]
  chunks+=1
 if cz%8==0:print('Chunks',chunks,flush=True)
np.save(P/'grid-before.npy',G);(P/'palette.json').write_text(json.dumps({'names':names,'bounds':[X0,X1,Y0,Y1,Z0,Z1]}))
marker=np.argwhere(G==ids.get('minecraft:red_wool',-1));marks=[{'x':int(x+X0),'y':int(y+Y0),'z':int(z+Z0)} for z,y,x in marker]
(P/'red-markers.json').write_text(json.dumps(marks));print('Red wool blocks',len(marks), 'bounds',[(min(a[k] for a in marks),max(a[k] for a in marks)) for k in ['x','y','z']] if marks else None,flush=True)
air=np.isin(G,[ids.get('minecraft:'+n,-1) for n in ['air','cave_air','void_air']]);top=np.where(~air,np.arange(Y0,Y1+1)[None,:,None],Y0-1).max(axis=1)
notbed_names={'air','cave_air','void_air','water','bubble_column','lava','ice','packed_ice','blue_ice','kelp','kelp_plant','seagrass','tall_seagrass','snow','glow_lichen','vine','cave_vines','cave_vines_plant','hanging_roots','sculk_vein'};notbed=np.array([n.removeprefix('minecraft:') in notbed_names or n.endswith('_leaves') for n in names]);bed=np.where(~notbed[G[:,:126,:]],np.arange(-64,62)[None,:,None],-65).max(axis=1)
craftids=[i for i,n in enumerate(names) if any(k in n for k in ['_planks','_stairs','_slab','_fence','_door','_trapdoor','lantern','iron_chain','barrel','chest','rail','stone_bricks','tuff_bricks','polished','_wool','iron_bars','_cauldron','bricks']) and n!='minecraft:red_wool']
craft=np.isin(G,craftids).any(axis=1);np.savez_compressed(P/'surface.npz',top=top,bed=bed,craft=craft)
v=np.clip(top*.9+35,15,245).astype('uint8');rgb=np.stack([v,v,v],axis=2);rgb[G[:,126,:]==ids.get('minecraft:water',-1)]=[25,90,160];rgb[craft]=[235,145,30]
for a in marks:rgb[a['z']-Z0,a['x']-X0]=[220,30,230]
im=Image.fromarray(rgb).resize((G.shape[2]*2,G.shape[0]*2));d=ImageDraw.Draw(im)
for x in range(-650,-454,20):
 px=(x-X0)*2;d.line((px,0,px,G.shape[0]*2),fill=(80,80,90));d.text((px+2,5),str(x),fill='white')
for z in range(2970,3156,20):
 pz=(z-Z0)*2;d.line((0,pz,G.shape[2]*2,pz),fill=(80,80,90));d.text((2,pz+2),str(z),fill='white')
im.save(P/'survey.png');print('DONE',chunks,'chunks',len(names),'materials',flush=True)
