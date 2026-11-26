import { HdsAnimationMode, HdsTabs, HdsTabsController, hdsMaterial } from '@kit.UIDesignKit';

import { AccountPage } from './AccountPage';
import { ManagePage } from './ManagePage';
import { MapPage } from './MapPage';
import { ShopPage } from './ShopPage';
import { CART, SESSION } from '../services/Api';

PersistentStorage.persistProp(SESSION, '');
PersistentStorage.persistProp(CART, '[]');
const DOCK_CLEARANCE: number = 76;

function serviceTabBar(icon: Resource, title: string): BottomTabBarStyle {
  return BottomTabBarStyle.of(icon, title)
    .iconStyle({
      selectedColor: $r('sys.color.ohos_id_color_activated'),
      unselectedColor: $r('sys.color.font_primary')
    })
    .labelStyle({
      selectedColor: $r('sys.color.ohos_id_color_activated'),
      unselectedColor: $r('sys.color.font_primary')
    });
}

@Entry
@Component
struct Index {
  private tabs: HdsTabsController = new HdsTabsController();
  @State selectedTab: number = 0;
  @State mapTarget: string = '';
  @State shopSearch: string = '';
  @State mapFullScreen: boolean = false;

  onBackPressed(): boolean {
    if (this.mapFullScreen) {
      this.toggleMapFullScreen();
      return true;
    }
    if (this.selectedTab === 0) {
      return false;
    }
    this.tabs.changeIndex(0);
    return true;
  }

  private toggleMapFullScreen(): void {
    this.mapFullScreen = !this.mapFullScreen;
    if (this.mapFullScreen) {
      this.tabs.applyHideAnimation(HdsAnimationMode.CLICK_ANIMATION);
    } else {
      this.tabs.applyShowAnimation(HdsAnimationMode.CLICK_ANIMATION);
    }
  }

  @Builder
  home() {
    Stack() {
      Image($r('app.media.world_night'))
        .width('100%')
        .height('100%')
        .objectFit(ImageFit.Cover)
        .accessibilityLevel('no')

      Column()
        .width('100%')
        .height('100%')
        .linearGradient({
          direction: GradientDirection.Bottom,
          colors: [['#AA081210', 0], ['#18081210', 0.55], ['#18081210', 1]]
        })
        .hitTestBehavior(HitTestMode.None)

      Column() {
        Text('流浪世界服务')
          .fontColor(Color.White)
          .fontSize(36)
          .fontWeight(FontWeight.Bold)
        Button('打开地图')
          .margin({ top: 22 })
          .height(48)
          .padding({ left: 24, right: 24 })
          .fontColor(Color.White)
          .backgroundColor('#1D2B26')
          .onClick(() => this.tabs.changeIndex(2))
      }
      .width('100%')
      .height('100%')
      .padding({ top: 32, left: 24, right: 24, bottom: DOCK_CLEARANCE })
      .alignItems(HorizontalAlign.Start)
    }
    .width('100%')
    .height('100%')
  }

  build() {
    HdsTabs({ controller: this.tabs }) {
      TabContent() {
        this.home()
      }.tabBar(serviceTabBar($r('app.media.nav_home'), '主页'))
      TabContent() {
        ShopPage({ mapSearch: this.shopSearch, onAccount: () => this.tabs.changeIndex(4) })
      }.tabBar(serviceTabBar($r('app.media.nav_shop'), '商店'))
      TabContent() {
        MapPage({
          target: this.mapTarget, active: this.selectedTab === 2, clean: this.mapFullScreen,
          onToggleClean: () => this.toggleMapFullScreen(),
          onAccount: () => this.tabs.changeIndex(4),
          onShopSearch: (label: string) => {
            this.shopSearch = label;
            this.tabs.changeIndex(1);
          }
        })
      }.tabBar(serviceTabBar($r('app.media.nav_map'), '地图'))
      TabContent() {
        ManagePage({
          onAccount: () => this.tabs.changeIndex(4),
          onMap: (url: string) => {
            this.mapTarget = url;
            this.tabs.changeIndex(2);
          }
        })
      }.tabBar(serviceTabBar($r('app.media.nav_manage'), '管理'))
      TabContent() {
        AccountPage()
      }.tabBar(serviceTabBar($r('app.media.nav_account'), '账户'))
    }
    .width('100%')
    .height('100%')
    .backgroundColor(this.selectedTab === 2 ? '#171719' : $r('sys.color.comp_background_primary'))
    .scrollable(false)
    .animationDuration(320)
    .barPosition(BarPosition.End)
    .barOverlap(true)
    .barFloatingStyle({
      adaptToHandedness: true,
      systemMaterialEffect: {
        materialType: hdsMaterial.MaterialType.ADAPTIVE,
        materialLevel: hdsMaterial.MaterialLevel.ADAPTIVE
      }
    })
    .onChange(index => {
      this.selectedTab = index;
    })
  }
}
