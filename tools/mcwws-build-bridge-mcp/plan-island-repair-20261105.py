import io,json,zlib,collections,shutil
from pathlib import Path
import numpy as np
from scipy import ndimage as ndi
P=Path('tools/mcwws-build-bridge-mcp/island-repair-20261105');S=Path('tools/mcwws-build-bridge-mcp/purple-coast-expansion-20261105')
meta=json.loads((S/'palette.json').read_text());names=json.loads((S/'target-palette.json').read_text());ids={n:i for i,n in enumerate(names)}
g=np.load(S/'grid-before.npy',mmap_mode='r');current=np.load(S/'grid-target.npy',mmap_mode='r');q=current.copy();D=np.load(S/'design.npz');H,W=g.shape[0],g.shape[2]
airids=[ids[n] for n in ['minecraft:air','minecraft:cave_air','minecraft:void_air'] if n in ids];water=ids['minecraft:water']
land=~np.isin(g[:,126,:],airids+[water]);lab,n=ndi.label(land);k=lab[3067-2940,-575+920];island=lab==k
distance=ndi.distance_transform_edt(~island);radius=45
buffer=(distance>0)&(distance<radius)&(current[:,126,:]==water)&~D['protect']
# Exact restoration of the isolated original island, including its original plants.
for iz,ix in zip(*np.where(island)):q[iz,:,ix]=g[iz,:,ix]
exclude={'air','cave_air','void_air','water','bubble_column','lava','ice','packed_ice','blue_ice','kelp','kelp_plant','seagrass','tall_seagrass','snow','glow_lichen','vine','cave_vines','cave_vines_plant','hanging_roots','sculk_vein'}
solid=np.array([n.removeprefix('minecraft:') not in exclude and not n.endswith('_leaves') for n in names]);originalbed=np.where(solid[g[:,:126,:]],np.arange(-64,62)[None,:,None],-65).max(axis=1);nowbed=np.where(solid[current[:,:126,:]],np.arange(-64,62)[None,:,None],-65).max(axis=1)
weight=np.clip(1-distance/radius,0,1);weight=weight*weight*(3-2*weight)
bed=np.maximum(nowbed,np.rint(weight*originalbed+(1-weight)*nowbed));bed=np.minimum(bed,60).astype('int16')
terrain={'air','cave_air','void_air','water','bubble_column','stone','andesite','diorite','granite','deepslate','tuff','calcite','dirt','coarse_dirt','rooted_dirt','grass_block','podzol','sand','red_sand','sandstone','gravel','clay','mud','packed_mud','blackstone','smooth_basalt','basalt','moss_block','moss_carpet','dripstone_block','pointed_dripstone','sculk','sculk_vein','amethyst_block','budding_amethyst','magma_block','seagrass','tall_seagrass','kelp','kelp_plant','glow_lichen','cave_vines','cave_vines_plant','hanging_roots','light_gray_concrete','gray_concrete'}
allowed=np.array([n.removeprefix('minecraft:') in terrain or n.endswith('_ore') for n in names])
for iz,ix in zip(*np.where(buffer)):
 h=int(bed[iz,ix]);old=int(nowbed[iz,ix]);col=q[iz,:,ix]
 for y in range(max(-63,old+1),h+1):
  iy=y+64
  if allowed[col[iy]]:col[iy]=ids['minecraft:gravel'] if y==h else ids['minecraft:deepslate'] if y<0 else ids['minecraft:stone']
 # Re-establish the original nearby foliage extending past the island's rock edge.
 if distance[iz,ix]<12:
  for iy in range(127,159):
   n=names[int(g[iz,iy,ix])]
   if n.endswith('_leaves') and current[iz,iy,ix]!=g[iz,iy,ix]:col[iy]=g[iz,iy,ix]
np.save(P/'target.npy',q);np.savez_compressed(P/'mask.npz',island=island,buffer=buffer)
change=np.where(q!=current);z,y,x=change
assert np.array_equal(q[:,159:,:],current[:,159:,:]),'Roof modified'
assert np.array_equal(q[:,126,:][~island],current[:,126,:][~island]),'Sea surface outside island modified'
assert all(np.array_equal(q[iz,:,ix],g[iz,:,ix]) for iz,ix in zip(*np.where(island)))
# Recover full original block states from the saved native chunk palettes.
import nbtlib
chunks={};regions={}
def originalstate(wx,wy,wz,expected):
 cx,cz=wx//16,wz//16;key=(cx,cz)
 if key not in chunks:
  rk=(cx//32,cz//32)
  if rk not in regions:regions[rk]=(S/'region-before'/f'r.{rk[0]}.{rk[1]}.mca').read_bytes()
  raw=regions[rk];off=(int.from_bytes(raw[(cx%32+cz%32*32)*4:(cx%32+cz%32*32)*4+4],'big')>>8)*4096;n=int.from_bytes(raw[off:off+4],'big');c=nbtlib.File.parse(io.BytesIO(zlib.decompress(raw[off+5:off+4+n])));secs={}
  for sec in c['sections']:
   st=sec.get('block_states')
   if not st:continue
   states=[];bare=[]
   for p in st['palette']:
    name=str(p['Name']);props=p.get('Properties',{});bare.append(name);states.append(name+('['+','.join(f'{k}={v}' for k,v in sorted(props.items()))+']' if props else ''))
   b=max(4,(len(states)-1).bit_length());per=64//b;data=[int(v)&((1<<64)-1) for v in st.get('data',[])];secs[int(sec['Y'])]=(states,bare,b,per,data)
  chunks[key]=secs
 st=chunks[key].get(wy//16)
 if not st:return expected
 states,bare,b,per,data=st;idx=(wy%16)*256+(wz%16)*16+wx%16;pi=(data[idx//per]>>((idx%per)*b))&((1<<b)-1) if data else 0
 return states[pi] if bare[pi]==expected else expected
states=[];sid={};records=[];samples=[];rng=np.random.default_rng(5753067);sampleindices=set(map(int,rng.choice(len(x),min(1000,len(x)),replace=False)))
for n,(iz,iy,ix) in enumerate(zip(z,y,x)):
 wx,wy,wz=int(ix-920),int(iy-64),int(iz+2940);name=names[int(q[iz,iy,ix])]
 state=originalstate(wx,wy,wz,name) if island[iz,ix] or (wy>=63 and distance[iz,ix]<12) else name
 if state not in sid:sid[state]=len(states);states.append(state)
 records.append((int(ix),int(iy),int(iz),sid[state]))
 if n in sampleindices:samples.append({'x':wx,'y':wy,'z':wz,'before':names[int(current[iz,iy,ix])],'after':state})
np.asarray(records,dtype='<u2').tofile(P/'placements.u16');(P/'states.json').write_text(json.dumps(states));
protected=json.loads((S/'audit.json').read_text());protect=protected['structures']+protected['roofs']+protected['markers'][::5]
tops=[]
for iz,ix in zip(*np.where(island)):
 if (ix*13+iz*19)%29:continue
 soil=[ids[n] for n in ['minecraft:grass_block','minecraft:dirt_path','minecraft:dirt','minecraft:coarse_dirt','minecraft:rooted_dirt'] if n in ids];yy=np.where(np.isin(g[iz,127:159,ix],soil))[0]
 if len(yy):iy=int(yy[-1]+127);tops.append({'x':int(ix-920),'y':iy-64,'z':int(iz+2940),'block':originalstate(int(ix-920),iy-64,int(iz+2940),names[int(g[iz,iy,ix])])})
(P/'audit.json').write_text(json.dumps({'changes':samples,'protected':protect,'tops':tops}))
report={'placements':len(records),'island_columns':int(island.sum()),'island_bounds':[int(np.where(island)[1].min()-920),int(np.where(island)[1].max()-920),int(np.where(island)[0].min()+2940),int(np.where(island)[0].max()+2940)],'write_bounds':[int(x.min()-920),int(x.max()-920),int(y.min()-64),int(y.max()-64),int(z.min()+2940),int(z.max()+2940)],'surface_outside_island_preserved':True,'tops_samples':len(tops),'full_original_states_restored':True}
(P/'plan.json').write_text(json.dumps(report,indent=2));print(json.dumps(report),flush=True)
