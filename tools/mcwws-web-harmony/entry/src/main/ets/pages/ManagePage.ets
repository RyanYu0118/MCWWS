import { errorMessage, request, SESSION } from '../services/Api';
import { BOTTOM_INSET, LoadState, ScreenHeader, SERVICE_CARD_COLOR, SERVICE_SHEET_COLOR } from '../components/NativeUi';

interface ShopLocation {
  viewUrl?: string;
  description?: string;
  enabled?: boolean;
  displayName?: string;
  world?: string;
  map?: string;
  x?: number;
  y?: number;
  z?: number;
  yaw?: number;
  pitch?: number;
  zoom?: number;
}
interface Store {
  id: string;
  titleResolved: string;
  itemCount: number;
  location?: ShopLocation;
}

@Component
export struct ManagePage {
  @StorageLink(SESSION) @Watch('sessionChanged') token: string = '';
  @State stores: Store[] = [];
  @State error: string = '';
  @State loading: boolean = false;
  @State query: string = '';
  @State editor: boolean = false;
  @State chosen: Store | null = null;
  @State viewUrl: string = '';
  @State note: string = '';
  @State markerVisible: boolean = true;
  @State saving: boolean = false;
  @State saveMessage: string = '';
  onAccount: () => void = () => {};
  onMap: (url: string) => void = () => {};
  private revision: number = 0;

  aboutToAppear(): void {
    this.sessionChanged();
  }

  sessionChanged(): void {
    this.stores = [];
    this.editor = false;
    this.chosen = null;
    this.error = '';
    void this.reload();
  }

  private async reload(): Promise<void> {
    const revision = ++this.revision;
    const token = this.token;
    this.loading = !!token;
    this.error = '';
    if (!token) {
      return;
    }
    try {
      const result = await request<Store[]>('/api/admin/shops', token);
      if (revision === this.revision) {
        this.stores = result;
      }
    } catch (err) {
      if (revision === this.revision) {
        this.error = errorMessage(err as Error);
      }
    } finally {
      if (revision === this.revision) {
        this.loading = false;
      }
    }
  }

  private edit(store: Store): void {
    this.chosen = store;
    this.viewUrl = store.location?.viewUrl || '';
    this.note = store.location?.description || '';
    this.markerVisible = store.location?.enabled !== false;
    this.saveMessage = '';
    this.editor = true;
  }

  private async save(): Promise<void> {
    const shop = this.chosen;
    const token = this.token;
    if (this.saving || !shop || !token) {
      return;
    }
    this.saving = true;
    this.saveMessage = '';
    const current = shop.location;
    const location: ShopLocation = {
      viewUrl: this.viewUrl.trim(),
      description: this.note.trim(),
      enabled: this.markerVisible,
      displayName: current?.displayName,
      world: current?.world,
      map: current?.map,
      x: current?.x,
      y: current?.y,
      z: current?.z,
      yaw: current?.yaw,
      pitch: current?.pitch,
      zoom: current?.zoom
    };
    try {
      await request<object>('/api/admin/shop-locations/' + encodeURIComponent(shop.id), token, location);
      if (token === this.token) {
        this.editor = false;
        await this.reload();
      }
    } catch (err) {
      this.saveMessage = errorMessage(err as Error);
    } finally {
      this.saving = false;
    }
  }

  @Builder
  editorContent() {
    Scroll() {
      Column({ space: 14 }) {
        ScreenHeader({ title: this.chosen?.titleResolved || '商店位置' })
        Text('BlueMap 链接').fontSize(14).width('100%')
        TextInput({ placeholder: '粘贴含世界和坐标的链接', text: this.viewUrl })
          .height(48).onChange(value => this.viewUrl = value)
        if (this.viewUrl.includes('#')) {
          Button('在地图上预览', { buttonStyle: ButtonStyleMode.TEXTUAL }).onClick(() => {
            this.editor = false;
            this.onMap(this.viewUrl);
          })
        }
        Text('备注').fontSize(14).width('100%')
        TextArea({ placeholder: '填写地图上的说明', text: this.note })
          .height(84).onChange(value => this.note = value)
        Row() {
          Text('在地图上显示').layoutWeight(1)
          Toggle({ type: ToggleType.Switch, isOn: this.markerVisible })
            .onChange(value => this.markerVisible = value)
        }.width('100%')
        if (this.saveMessage) {
          Text(this.saveMessage).fontSize(14).fontColor($r('sys.color.font_secondary'))
        }
        Button(this.saving ? '保存中' : '保存')
          .width('100%').height(48).enabled(!this.saving).onClick(() => void this.save())
      }.width('100%').padding(20)
    }
  }

  build() {
    Column() {
      List({ space: 8 }) {
        ListItem() { ScreenHeader({ title: '管理', caption: '商店位置' }) }
        if (!this.token) {
          ListItem() {
            LoadState({ message: '登录后可查看管理权限', actionLabel: '前往账户', onAction: this.onAccount })
          }
        } else if (this.error || (!this.stores.length && this.loading)) {
          ListItem() {
            LoadState({ loading: this.loading, message: this.error, actionLabel: '重试',
              onAction: () => void this.reload() })
          }
        } else {
          ListItem() {
            Row({ space: 8 }) {
              Search({ placeholder: '搜索商店', value: this.query })
                .layoutWeight(1).onChange(value => this.query = value)
              Button('刷新', { buttonStyle: ButtonStyleMode.TEXTUAL }).enabled(!this.loading)
                .onClick(() => void this.reload())
            }.padding({ left: 16, right: 16, bottom: 10 })
          }
          ForEach(this.stores.filter(shop => (shop.titleResolved + ' ' + shop.id).toLowerCase().includes(
            this.query.trim().toLowerCase())), (shop: Store) => {
            ListItem() {
              Row({ space: 10 }) {
                Column({ space: 5 }) {
                  Text(shop.titleResolved).fontSize(17).fontWeight(FontWeight.Medium)
                  Text(shop.itemCount + ' 件商品  ·  ' + (shop.location?.viewUrl ? '已配置' : '未配置'))
                    .fontSize(13).fontColor($r('sys.color.font_secondary'))
                }.layoutWeight(1).alignItems(HorizontalAlign.Start)
                Button('编辑', { buttonStyle: ButtonStyleMode.TEXTUAL }).onClick(() => this.edit(shop))
              }.padding(14).borderRadius(16).backgroundColor(SERVICE_CARD_COLOR)
            }.padding({ left: 16, right: 16 })
          }, (shop: Store) => shop.id)
        }
      }
      .width('100%').layoutWeight(1).cachedCount(4)
      .contentEndOffset(BOTTOM_INSET)
      .clipContent(ContentClipMode.SAFE_AREA)
      .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
    }
    .width('100%').height('100%')
    .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
    .bindSheet($$this.editor, this.editorContent(), {
      height: SheetSize.LARGE, dragBar: true, showClose: true, backgroundColor: SERVICE_SHEET_COLOR
    })
  }
}
