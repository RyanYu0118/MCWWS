import io,json,zlib,collections
from pathlib import Path
import numpy as np
import nbtlib
from scipy import ndimage as ndi
from PIL import Image,ImageDraw
P=Path('tools/mcwws-build-bridge-mcp/ocean-bed-refinement-20261105');P.mkdir(exist_ok=True)
X0,X1,Z0,Z1,Y0,Y1=-1200,-560,2800,3460,-64,111
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
exclude=['air','cave_air','void_air','water','bubble_column','lava','ice','packed_ice','blue_ice','kelp','kelp_plant','seagrass','tall_seagrass','snow','glow_lichen','vine','cave_vines','cave_vines_plant','hanging_roots','sculk_vein','lily_pad']
notbed=np.array([n.removeprefix('minecraft:') in exclude or n.endswith('_leaves') for n in names]);solids=~notbed[G[:,:62-Y0,:]]
bed=np.where(solids,np.arange(Y0,62)[None,:,None],Y0-1).max(axis=1)
old=np.load('tools/mcwws-build-bridge-mcp/ocean-restoration-20261105/design.npz');cut=old['cut'][:Z1-Z0+1,:X1-X0+1]
np.save(P/'bed.npy',bed);report={}
for label,mask in [('native_reference',np.indices(bed.shape)[0]<220),('cut_bay',cut),('player_reference',np.fromfunction(lambda z,x:((x+X0+832)**2+(z+Z0-2966)**2)<50**2,bed.shape))]:
 v=bed[mask];report[label]={'percentiles':np.quantile(v,[0,.1,.25,.5,.75,.9,1]).tolist(),'columns':len(v)}
print(json.dumps(report));(P/'survey-report.json').write_text(json.dumps(report,indent=2))
v=np.clip((bed+50)*2,0,255).astype('uint8');rgb=np.stack([v,255-v,np.full_like(v,130)],axis=2);im=Image.fromarray(rgb).resize((bed.shape[1]*2,bed.shape[0]*2));d=ImageDraw.Draw(im)
for x in range(-1200,-559,100):
 px=(x-X0)*2;d.line((px,0,px,bed.shape[0]*2),fill=(90,90,90));d.text((px+2,5),str(x),fill='white')
for z in range(2800,3461,100):
 pz=(z-Z0)*2;d.line((0,pz,bed.shape[1]*2,pz),fill=(90,90,90));d.text((2,pz+3),str(z),fill='white')
im.save(P/'bed-survey.png');print('DONE',chunks,'chunks',len(names),'materials',flush=True)
