import json,sqlite3,datetime,collections,shutil
from pathlib import Path
import numpy as np
from scipy import ndimage as ndi
from PIL import Image,ImageDraw

P=Path('tools/mcwws-build-bridge-mcp/ocean-restoration-20261105')
meta=json.loads((P/'palette.json').read_text());names=meta['names'];ids={n:i for i,n in enumerate(names)}
g=np.load(P/'grid-before.npy',mmap_mode='r');H,W=g.shape[0],g.shape[2];X0=-1200;Z0=2800
zz,xx=np.mgrid[Z0:Z0+H,X0:X0+W]
surf=np.load(P/'surface.npz');top=surf['top'];water=surf['water']
air=np.isin(g,[ids.get('minecraft:'+n,-1) for n in ['air','cave_air','void_air']])
natural_full=['stone','andesite','diorite','granite','deepslate','tuff','calcite','dirt','coarse_dirt','rooted_dirt','grass_block','podzol','sand','red_sand','sandstone','red_sandstone','gravel','clay','mud','packed_mud','blackstone','smooth_basalt','basalt','moss_block','snow_block','powder_snow','ice','packed_ice','blue_ice','dripstone_block','sculk','amethyst_block','budding_amethyst','magma_block']
solid_ids=[ids['minecraft:'+n] for n in natural_full if 'minecraft:'+n in ids]+[i for i,n in enumerate(names) if n.endswith('_ore')]
solids=np.isin(g,solid_ids)
ground=np.where(solids,np.arange(192)[None,:,None],-1).max(axis=1).astype(float)
floor=np.where(solids[:,:63,:],np.arange(63)[None,:,None],-1).max(axis=1).astype(float)
craft=np.load(P/'structure-columns.npy');protect=ndi.distance_transform_edt(~craft)<=12
# Preserve the complete recessed port and cliff, including its submerged foundations.
port=(xx>=-725)&(zz<=3160)
protect |= port
history=np.zeros((H,W),bool)
c=sqlite3.connect(Path('plugins/FastAsyncWorldEdit/history/world/summary.db').resolve().as_uri()+'?mode=ro',uri=True);c.row_factory=sqlite3.Row
for r in c.execute("select * from _edits where time>=? and command like 'sphere air%' and size>0",(1793376000,)):
 x1=max(r['x1'],X0);x2=min(r['x2'],X0+W-1);z1=max(r['z1'],Z0);z2=min(r['z2'],Z0+H-1)
 if x1<=x2 and z1<=z2:history[z1-Z0:z2-Z0+1,x1-X0:x2-X0+1]=True
# Include the prior flat quarry cuts, which are evident in the existing surface.
flat=(ground<=58)&(ground>=35)&(zz>=2958)&(zz<=3435)&(xx<=-735)
raw=history|flat
raw=ndi.binary_fill_holes(ndi.binary_closing(raw,iterations=18))
field=ndi.gaussian_filter(raw.astype(float),8)
cut=(field>.28)&(xx>=-1185)&(xx<=-690)&(zz>=2940)&(zz<=3450)
cut &= ~protect
# Enclose no isolated islands: keep only the principal excavation.
lab,n=ndi.label(cut);sizes=np.bincount(lab.ravel());sizes[0]=0;cut=lab==sizes.argmax()
dist=ndi.distance_transform_edt(cut)
# Coast transitions retain original mountains and smoothly descend into a varied basin.
variation=(2.4*np.sin(xx/47+zz/79)+1.5*np.cos(zz/31-xx/67)+.9*np.sin(xx/19+zz/27))
deep=29+variation
nearshore=59-np.minimum(dist*.62,29)+variation*.35
desired=np.maximum(deep,nearshore)
original=ground.copy()
# Where a roof stands over a water-level opening, preserve the entire roof.
overhang=(ground>=95)&(air[:,70:85,:].sum(axis=1)>=5)
original[overhang]=floor[overhang]
weight=np.clip(dist/24,0,1);weight=weight*weight*(3-2*weight)
target=original*(1-weight)+desired*weight
target=np.rint(target).astype(np.int16)
target[~cut]=floor[~cut].astype(np.int16)
# Blend the newly deepened basin into the existing northern seabed over 45 blocks.
north_blend=np.clip((zz-2950)/48,0,1)
target=np.where(cut&(zz<2998),np.rint(target*north_blend+floor*(1-north_blend)),target).astype(np.int16)
target=np.maximum(target,18)
# Existing northern seawater lowered to 54 is restored, rather than becoming a step.
restore=(water>=45)&(water<62)&(zz>=2800)&(zz<=3170)&(xx<=-580)
restore &= air[:,55:63,:].all(axis=1)
np.savez_compressed(P/'design.npz',cut=cut,target=target,protect=protect,overhang=overhang,restore=restore,ground=ground,floor=floor,dist=dist)
rgb=np.stack([np.clip(top+40,20,255)]*3,axis=2).astype('uint8');rgb[water>=top]=[25,80,140]
rgb[cut]=np.stack([np.full(cut.sum(),35),np.clip(target[cut]*2,40,150),np.full(cut.sum(),210)],axis=1)
rgb[restore]=[30,180,230];rgb[protect&((zz>2930)&(xx>-800))]=[220,150,30]
im=Image.fromarray(rgb).resize((W*2,H*2));d=ImageDraw.Draw(im)
for x in range(-1200,-439,100):
 px=(x-X0)*2;d.line((px,0,px,H*2),fill=(90,90,90));d.text((px+2,5),str(x),fill='white')
for z in range(2800,3501,100):
 pz=(z-Z0)*2;d.line((0,pz,W*2,pz),fill=(90,90,90));d.text((2,pz+3),str(z),fill='white')
im.save(P/'design.png')
report={'cut_columns':int(cut.sum()),'restore_columns':int(restore.sum()),'protected_columns':int(protect.sum()),'target_range':[int(target[cut].min()),int(target[cut].max())],'cut_bounds':[int(xx[cut].min()),int(xx[cut].max()),int(zz[cut].min()),int(zz[cut].max())],'ground_quantiles':np.quantile(ground[cut],[0,.1,.5,.9,.99,1]).tolist(),'overhangs_preserved':int((overhang&cut).sum())}
(P/'design-report.json').write_text(json.dumps(report,indent=2));print(json.dumps(report))
