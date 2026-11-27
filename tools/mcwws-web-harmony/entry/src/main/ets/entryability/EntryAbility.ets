import { ConfigurationConstant, UIAbility } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { window } from '@kit.ArkUI';

export default class EntryAbility extends UIAbility {
  onCreate(): void {
    try {
      this.context.getApplicationContext().setColorMode(ConfigurationConstant.ColorMode.COLOR_MODE_DARK);
    } catch {
      hilog.warn(0x0000, 'MCWWS', 'Unable to apply the dark application palette');
    }
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    windowStage.loadContent('pages/Index', error => {
      if (error.code) {
        hilog.error(0x0000, 'MCWWS', 'Unable to load the start page. Code: %{public}d', error.code);
        return;
      }
      windowStage.getMainWindow().then(mainWindow => mainWindow.setWindowSystemBarProperties({
        statusBarColor: '#00000000',
        statusBarContentColor: '#FFFFFF',
        navigationBarColor: '#00000000',
        navigationBarContentColor: '#FFFFFF'
      })).catch(() => {});
    });
  }
}
