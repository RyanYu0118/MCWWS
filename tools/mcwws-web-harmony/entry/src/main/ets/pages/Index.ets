import { HdsAnimationMode, HdsTabs, HdsTabsController, hdsMaterial } from '@kit.UIDesignKit';

import { AccountPage } from './AccountPage';
import { ManagePage } from './ManagePage';
import { MapPage } from './MapPage';
import { ShopPage } from './ShopPage';
import { CART, SESSION } from '../services/Api';
import { BOTTOM_INSET } from '../components/NativeUi';

PersistentStorage.persistProp(SESSION, '');
PersistentStorage.persistProp(CART, '[]');
function serviceTabBar(icon: Resource, title: string): BottomTabBarStyle {
  return BottomTabBarStyle.of(icon, title)
    .iconStyle({
      selectedColor: Color.White,
      unselectedColor: '#CCFFFFFF'
    })
    .labelStyle({
      selectedColor: Color.White,
      unselectedColor: '#CCFFFFFF'
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
    .padding({ top: 32, left: 24, right: 24, bottom: BOTTOM_INSET })
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  serviceTabs() {
    HdsTabs({ controller: this.tabs }) {
      TabContent() {
        this.home()
      }.tabBar(serviceTabBar($r('app.media.nav_home'), '主页'))
      TabContent() {
        ShopPage({ mapSearch: this.shopSearch, onAccount: () => this.tabs.changeIndex(4) })
      }
      .clip(false)
      .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
      .tabBar(serviceTabBar($r('app.media.nav_shop'), '商店'))
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
      }
      .clip(false)
      .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
      .tabBar(serviceTabBar($r('app.media.nav_map'), '地图'))
      TabContent() {
        ManagePage({
          onAccount: () => this.tabs.changeIndex(4),
          onMap: (url: string) => {
            this.mapTarget = url;
            this.tabs.changeIndex(2);
          }
        })
      }
      .clip(false)
      .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
      .tabBar(serviceTabBar($r('app.media.nav_manage'), '管理'))
      TabContent() {
        AccountPage()
      }
      .clip(false)
      .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
      .tabBar(serviceTabBar($r('app.media.nav_account'), '账户'))
    }
    .width('100%')
    .height('100%')
    .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
    .backgroundColor(Color.Transparent)
    .scrollable(false)
    .animationDuration(320)
    .barPosition(BarPosition.End)
    .barOverlap(true)
    .barFloatingStyle({
      adaptToHandedness: true,
      gradientMask: { maskColor: Color.Transparent, maskHeight: 0 },
      systemMaterialEffect: {
        materialType: hdsMaterial.MaterialType.ADAPTIVE,
        materialLevel: hdsMaterial.MaterialLevel.ADAPTIVE
      }
    })
    .onChange(index => {
      this.selectedTab = index;
    })
  }

  build() {
    Stack() {
      Image($r('app.media.world_night'))
        .width('100%').height('100%').objectFit(ImageFit.Cover)
        .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
        .hitTestBehavior(HitTestMode.None)
        .accessibilityLevel('no')
      Column()
        .width('100%').height('100%')
        .linearGradient({ direction: GradientDirection.Bottom,
          colors: [['#8509100E', 0], ['#5C09100E', 0.53], ['#8B09100E', 1]] })
        .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
        .hitTestBehavior(HitTestMode.None)
      this.serviceTabs()
    }
    .width('100%').height('100%')
    .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
  }
}
