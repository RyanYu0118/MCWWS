import json,shutil,collections
from pathlib import Path
import numpy as np
from scipy import ndimage as ndi
P=Path('tools/mcwws-build-bridge-mcp/ocean-bed-refinement-20261105');meta=json.loads((P/'palette.json').read_text());names=meta['names'];ids={n:i for i,n in enumerate(names)}
g=np.load(P/'grid-before.npy',mmap_mode='r');q=g.copy();D=np.load(P/'design.npz');domain=D['domain'];target=D['target'];H,W=domain.shape;Y0=-64
def bid(n):
 n='minecraft:'+n
 if n not in ids:ids[n]=len(names);names.append(n)
 return ids[n]
water=bid('water');gravel=bid('gravel');stone=bid('stone');deep=bid('deepslate');tuff=bid('tuff');sand=bid('sand');clay=bid('clay')
natural={'air','cave_air','void_air','water','bubble_column','lava','stone','andesite','diorite','granite','deepslate','tuff','calcite','dirt','coarse_dirt','rooted_dirt','grass_block','podzol','sand','red_sand','sandstone','red_sandstone','gravel','clay','mud','packed_mud','blackstone','smooth_basalt','basalt','moss_block','moss_carpet','dripstone_block','pointed_dripstone','sculk','sculk_vein','amethyst_block','budding_amethyst','magma_block','seagrass','tall_seagrass','kelp','kelp_plant','glow_lichen','vine','cave_vines','cave_vines_plant','hanging_roots','brown_mushroom','red_mushroom'}
natural.update({'short_grass','tall_grass','fern','large_fern','azalea','flowering_azalea','small_dripleaf','big_dripleaf','big_dripleaf_stem','sea_pickle','powder_snow','snow','snow_block','spore_blossom','cobweb','raw_copper_block','raw_iron_block','bone_block','obsidian','crying_obsidian','dead_bush','sugar_cane','lily_pad','pale_moss_block','pale_moss_carpet','leaf_litter','sweet_berry_bush'})
natural.update({'netherrack','small_amethyst_bud','medium_amethyst_bud','large_amethyst_bud','amethyst_cluster'})
allowed=np.array([n.removeprefix('minecraft:') in natural or n.endswith(('_ore','_leaves')) for n in names])
bed=np.load(P/'bed.npy');rng=np.random.default_rng(561120);patch=ndi.gaussian_filter(rng.normal(size=(H,W)),11);patch/=patch.std();rough=ndi.gaussian_filter(rng.normal(size=(H,W)),3);rough/=rough.std()
gx=np.gradient(target.astype(float),axis=1);gz=np.gradient(target.astype(float),axis=0);slope=np.hypot(gx,gz)
def put(col,a,b,v):
 a=max(-63,int(a))-Y0;b=min(61,int(b))-Y0
 if a>b:return
 aview=col[a:b+1];ok=allowed[aview];aview[ok]=v
for iz,ix in zip(*np.where(domain)):
 h=int(target[iz,ix]);oldh=int(bed[iz,ix]);col=q[iz,:,ix]
 base=deep if h<5 else stone
 if oldh<h:put(col,oldh+1,h-3,base)
 put(col,h-2,h-1,tuff if patch[iz,ix]>1.7 else base)
 cap=gravel
 if patch[iz,ix]>2.05:cap=sand if h>15 else clay
 elif slope[iz,ix]>1.45 and rough[iz,ix]>.8:cap=deep if h<10 else tuff
 put(col,h,h,cap)
 put(col,h+1,61,water)
np.save(P/'grid-target.npy',q);(P/'target-palette.json').write_text(json.dumps(names))
changes=np.where(q!=g);z,y,x=changes;newid=q[changes];records=np.column_stack((x,y,z,newid)).astype('<u2');records.tofile(P/'placements.u16')
counts=collections.Counter(names[int(i)] for i in newid);bds=[int(x.min()-1200),int(x.max()-1200),int(y.min()-64),int(y.max()-64),int(z.min()+2800),int(z.max()+2800)]
backup=P/'region-before';backup.mkdir(exist_ok=True)
for rx in range(bds[0]//512,bds[1]//512+1):
 for rz in range(bds[4]//512,bds[5]//512+1):
  src=Path('world/dimensions/minecraft/overworld/region')/f'r.{rx}.{rz}.mca'
  if src.exists() and not (backup/src.name).exists():shutil.copy2(src,backup/src.name)
assert (g[:,126:,:]==q[:,126:,:]).all(),'Sea surface or upper structure changed'
assert not ((g!=q)&~allowed[g]).any(),'Protected material replaced'
assert not ((g!=q)&D['port'][:,None,:]).any(),'Harbour changed'
assert (g[2966-2800,:, -832+1200]==q[2966-2800,:,-832+1200]).all(),'Reference changed'
def pos(iz,iy,ix,**extra):return {'x':int(ix-1200),'y':int(iy-64),'z':int(iz+2800),**extra}
sample=[]
for i in rng.choice(len(x),min(1000,len(x)),replace=False):sample.append(pos(z[i],y[i],x[i],before=names[int(g[z[i],y[i],x[i]])],after=names[int(q[z[i],y[i],x[i]])]))
# Structures, the water/ice surface, cliff roofs, and the player's reference seabed
# are all compared with their full live block states before and after sculpting.
protected=[]
for xx in range(-720,-580,7):
 for zz in range(2930,3161,7):
  ix=xx+1200;iz=zz-2800;ys=np.where(~allowed[g[iz,:,ix]])[0]
  if len(ys):protected.append(pos(iz,int(ys[-1]),ix,block=names[int(g[iz,ys[-1],ix])]))
for i in rng.choice(len(protected),min(220,len(protected)),replace=False):pass
protected=[protected[int(i)] for i in rng.choice(len(protected),min(220,len(protected)),replace=False)]
reference=[]
for i in range(100):
 xx=int(rng.integers(-850,-810));zz=int(rng.integers(2946,2986));ix=xx+1200;iz=zz-2800
 if domain[iz,ix]:continue
 iy=int(bed[iz,ix])+64;reference.append(pos(iz,iy,ix,block=names[int(g[iz,iy,ix])]))
surface=[]
for i in range(150):
 j=int(rng.integers(len(x)));surface.append(pos(z[j],126,x[j],block=names[int(g[z[j],126,x[j]])]))
(P/'audit.json').write_text(json.dumps({'changes':sample,'structures':protected,'reference':reference,'surface':surface}))
report={'changed_blocks':len(x),'bounds':bds,'materials':dict(counts),'protected_samples':len(protected),'reference_samples':len(reference),'surface_samples':len(surface),'sea_surface_preserved':True,'harbour_preserved':True,'reference_preserved':True}
(P/'write-plan.json').write_text(json.dumps(report,indent=2));print(json.dumps(report),flush=True)
