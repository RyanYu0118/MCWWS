import json,shutil,collections
from pathlib import Path
import numpy as np
from scipy import ndimage as ndi
P=Path('tools/mcwws-build-bridge-mcp/purple-coast-expansion-20261105');meta=json.loads((P/'palette.json').read_text());names=meta['names'];ids={n:i for i,n in enumerate(names)}
g=np.load(P/'grid-before.npy',mmap_mode='r');q=g.copy();D=np.load(P/'design.npz');domain=D['domain'];target=D['target'];clear_top=D['clear_top'];S=np.load(P/'surface.npz');bed=S['bed'];H,W=domain.shape
def bid(n):
 n='minecraft:'+n
 if n not in ids:ids[n]=len(names);names.append(n)
 return ids[n]
water=bid('water');air=bid('air');gravel=bid('gravel');stone=bid('stone');deep=bid('deepslate');tuff=bid('tuff');sand=bid('sand');clay=bid('clay')
natural={'air','cave_air','void_air','water','bubble_column','lava','stone','andesite','diorite','granite','deepslate','tuff','calcite','dirt','coarse_dirt','rooted_dirt','grass_block','podzol','sand','red_sand','sandstone','red_sandstone','gravel','clay','mud','packed_mud','blackstone','smooth_basalt','basalt','moss_block','moss_carpet','dripstone_block','pointed_dripstone','sculk','sculk_vein','amethyst_block','budding_amethyst','magma_block','seagrass','tall_seagrass','kelp','kelp_plant','glow_lichen','vine','cave_vines','cave_vines_plant','hanging_roots','brown_mushroom','red_mushroom','short_grass','tall_grass','fern','large_fern','azalea','flowering_azalea','small_dripleaf','big_dripleaf','big_dripleaf_stem','sea_pickle','powder_snow','snow','snow_block','spore_blossom','cobweb','raw_copper_block','raw_iron_block','bone_block','obsidian','crying_obsidian','dead_bush','sugar_cane','lily_pad','pale_moss_block','pale_moss_carpet','leaf_litter','sweet_berry_bush','netherrack','small_amethyst_bud','medium_amethyst_bud','large_amethyst_bud','amethyst_cluster','light_gray_concrete','gray_concrete'}
natural.update({'sculk_sensor','calibrated_sculk_sensor','sculk_shrieker','sculk_catalyst'})
allowed=np.array([n.removeprefix('minecraft:') in natural or n.endswith(('_ore','_leaves','_terracotta')) for n in names])
patch=ndi.gaussian_filter(np.random.default_rng(653211).normal(size=(H,W)),10);patch/=patch.std()
def put(col,a,b,v):
 a=max(-63,int(a))+64;b=min(94,int(b))+64
 if a>b:return
 view=col[a:b+1];ok=allowed[view];view[ok]=v
for iz,ix in zip(*np.where(domain)):
 h=int(target[iz,ix]);oldh=int(bed[iz,ix]);col=q[iz,:,ix]
 base=deep if h<4 else stone
 if h>oldh:put(col,oldh+1,h-3,base)
 put(col,h-2,h-1,tuff if patch[iz,ix]>1.7 else base)
 cap=gravel if patch[iz,ix]<1.9 else sand if h>25 else clay
 put(col,h,h,cap);put(col,h+1,62,water)
 put(col,63,int(clear_top[iz,ix]),air)
np.save(P/'grid-target.npy',q);(P/'target-palette.json').write_text(json.dumps(names))
assert np.array_equal(q[:,95+64:,:],g[:,95+64:,:]),'Upper mountain changed'
assert not ((q!=g)&~D['polygon'][:,None,:]).any(),'Guide boundary crossed'
assert not ((q!=g)&D['protect'][:,None,:]).any(),'Protected structure changed'
assert not ((q!=g)&~allowed[g]).any(),'Non-terrain material changed'
guides=json.loads((P/'selected-guide.json').read_text())
for a in guides:assert q[a['z']-2940,a['y']+64,a['x']+920]==g[a['z']-2940,a['y']+64,a['x']+920]
line=D['line']&~D['protect'];assert (q[:,126,:][line]==water).all(),'Water did not reach guide'
# Full 3D roof preservation, including the older low cave roofs.
for iz,ix in zip(*np.where(domain&(D['existing_roof']<95))):
 roof=int(D['existing_roof'][iz,ix]);assert np.array_equal(q[iz,roof+64:,ix],g[iz,roof+64:,ix]),'Low roof changed'
force=np.zeros_like(q,dtype=bool)
for iz,ix in zip(*np.where(domain)):
 a=int(target[iz,ix])-2+64;force[iz,a:127,ix]=allowed[q[iz,a:127,ix]]
unique_changes=int((q!=g).sum())
change=np.where((q!=g)|force);z,y,x=change;newid=q[change];rec=np.column_stack((x,y,z,newid)).astype('<u2');rec.tofile(P/'placements.u16')
bounds=[int(x.min()-920),int(x.max()-920),int(y.min()-64),int(y.max()-64),int(z.min()+2940),int(z.max()+2940)]
backup=P/'region-before';backup.mkdir(exist_ok=True)
for rx in range(bounds[0]//512,bounds[1]//512+1):
 for rz in range(bounds[4]//512,bounds[5]//512+1):
  src=Path('world/dimensions/minecraft/overworld/region')/f'r.{rx}.{rz}.mca'
  if src.exists() and not (backup/src.name).exists():shutil.copy2(src,backup/src.name)
rng=np.random.default_rng(631105)
def pos(iz,iy,ix,**extra):return {'x':int(ix-920),'y':int(iy-64),'z':int(iz+2940),**extra}
samples=[]
for n in rng.choice(len(x),min(1000,len(x)),replace=False):samples.append(pos(z[n],y[n],x[n],before=names[int(g[z[n],y[n],x[n]])],after=names[int(q[z[n],y[n],x[n]])]))
structures=[]
craftids=[i for i,n in enumerate(names) if any(k in n for k in ['planks','stairs','slab','fence','door','lantern','iron_chain','barrel','chest','rail','wool','iron_bars'])]
zz,xx=np.where(D['protect']&D['polygon'])
for n in rng.choice(len(xx),min(800,len(xx)),replace=False):
 iz,ix=zz[n],xx[n];ys=np.where(np.isin(g[iz,:,ix],craftids))[0]
 if len(ys):iy=int(ys[int(rng.integers(len(ys)))]);structures.append(pos(iz,iy,ix,block=names[int(g[iz,iy,ix])]))
if len(structures)>250:structures=[structures[int(n)] for n in rng.choice(len(structures),250,replace=False)]
roof=[]
for n in rng.choice(len(x),150,replace=False):
 iz,ix=z[n],x[n];iy=int(max(95,D['existing_roof'][iz,ix]))+64
 if iy>=384:iy=95+64
 roof.append(pos(iz,iy,ix,block=names[int(g[iz,iy,ix])]))
markers=[{'x':a['x'],'y':a['y'],'z':a['z'],'block':'minecraft:purple_wool'} for a in guides]
shore=[]
for iz,ix in zip(*np.where(line)):shore.append(pos(iz,126,ix,before=names[int(g[iz,126,ix])],after=names[int(q[iz,126,ix])]))
profiles=[]
for iz,ix in zip(*np.where(domain)):
 if (ix*17+iz*31)%107:continue
 h=int(target[iz,ix])
 for yy in [h-1,h,h+1,62]:profiles.append(pos(iz,yy+64,ix,block=names[int(q[iz,yy+64,ix])]))
(P/'audit.json').write_text(json.dumps({'changes':samples,'structures':structures,'roofs':roof,'markers':markers,'shore':shore,'profiles':profiles}))
materials=collections.Counter(names[int(v)] for v in newid);report={'changed_blocks':len(x),'unique_changed_blocks':unique_changes,'bounds':bounds,'materials':dict(materials),'new_sea_columns':int(((g[:,126,:]!=water)&(q[:,126,:]==water)).sum()),'guide_columns_reached':int(line.sum()),'guide_columns_protected':int((D['line']&D['protect']).sum()),'structure_samples':len(structures),'roof_samples':len(roof),'profile_samples':len(profiles),'curve_markers_preserved':True,'upper_mountain_preserved':True,'protected_structures_preserved':True}
(P/'write-plan.json').write_text(json.dumps(report,indent=2));print(json.dumps(report),flush=True)
