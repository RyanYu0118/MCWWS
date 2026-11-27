import { ApiError, dateText, errorMessage, money, ORIGIN, request, SESSION } from '../services/Api';
import { BOTTOM_INSET, LoadState, ScreenHeader, SERVICE_CARD_COLOR } from '../components/NativeUi';

interface AuthResult {
  username: string;
  playerId: string;
  authToken: string;
}
interface Economy {
  balanceFormatted?: string;
  balance: number | null;
  playHours?: number | null;
  role: string;
  online: boolean;
  warningProgress?: number;
  warningMax?: number;
}
interface LedgerEntry {
  id: string;
  createdAt: string;
  direction: string;
  description: string;
  categoryLabel: string;
  signedAmountFormatted: string;
  balanceAfterFormatted: string;
}
interface LedgerSummary {
  creditTotal: number;
  debitTotal: number;
  flightDebitTotal: number;
}
interface Ledger {
  entries: LedgerEntry[];
  summary: LedgerSummary;
  summaryAll?: LedgerSummary;
}
interface Credentials {
  username: string;
  password: string;
  playerId?: string;
}

@Component
export struct AccountPage {
  @StorageLink(SESSION) @Watch('sessionChanged') token: string = '';
  @State username: string = '';
  @State password: string = '';
  @State playerId: string = '';
  @State register: boolean = false;
  @State profile: AuthResult | null = null;
  @State economy: Economy | null = null;
  @State ledger: Ledger | null = null;
  @State error: string = '';
  @State busy: boolean = false;
  @State filter: string = 'exclude_flight';
  @State filterIndex: number = 0;
  private generation: number = 0;

  aboutToAppear(): void {
    this.sessionChanged();
  }

  sessionChanged(): void {
    this.generation++;
    this.economy = null;
    this.ledger = null;
    this.error = '';
    if (this.token) {
      void this.refresh();
    } else {
      this.profile = null;
      this.busy = false;
    }
  }

  private async signIn(): Promise<void> {
    if (this.busy) {
      return;
    }
    this.busy = true;
    this.error = '';
    const credentials: Credentials = { username: this.username.trim(), password: this.password };
    if (this.register) {
      credentials.playerId = this.playerId.trim();
    }
    try {
      const result = await request<AuthResult>(this.register ? '/api/register' : '/api/login', '', credentials);
      this.profile = result;
      this.password = '';
      this.token = result.authToken;
    } catch (err) {
      this.error = errorMessage(err as Error);
    } finally {
      if (!this.token) {
        this.busy = false;
      }
    }
  }

  private async signOut(): Promise<void> {
    const token = this.token;
    this.token = '';
    try {
      const body: Record<string, string> = {};
      await request<object>('/api/logout', token, body);
    } catch {
      // Local access has already been cleared.
    }
  }

  private report(error: Error, generation: number): void {
    if (generation !== this.generation) {
      return;
    }
    if (error instanceof ApiError && error.status === 401) {
      this.token = '';
    } else {
      this.error = errorMessage(error);
    }
  }

  private async refresh(): Promise<void> {
    const token = this.token;
    if (!token) {
      return;
    }
    const generation = ++this.generation;
    this.busy = true;
    this.error = '';
    const calls: Promise<void>[] = [
      request<AuthResult>('/api/profile', token).then(value => {
        if (generation === this.generation) this.profile = value;
      }).catch((error: Error) => this.report(error, generation)),
      request<Economy>('/api/player-economy', token).then(value => {
        if (generation === this.generation) this.economy = value;
      }).catch((error: Error) => this.report(error, generation)),
      request<Ledger>('/api/player-ledger?limit=60&filter=' + this.filter, token).then(value => {
        if (generation === this.generation) this.ledger = value;
      }).catch((error: Error) => this.report(error, generation))
    ];
    await Promise.all(calls);
    if (generation === this.generation) {
      this.busy = false;
    }
  }

  private playTimeLabel(): string {
    const hours = this.economy?.playHours;
    if (!this.economy) return '';
    return typeof hours === 'number' && Number.isFinite(hours) ? '游戏时长 ' + hours.toFixed(1) + ' 小时' :
      '游戏时长 —';
  }

  @Builder
  login() {
    Column({ space: 14 }) {
      TextInput({ placeholder: '用户名', text: this.username })
        .height(48)
        .enableKeyboardOnFocus(false)
        .onChange(value => this.username = value)
      TextInput({ placeholder: '密码', text: this.password })
        .type(InputType.Password)
        .height(48)
        .enableKeyboardOnFocus(false)
        .onChange(value => this.password = value)
      if (this.register) {
        TextInput({ placeholder: '游戏玩家 ID', text: this.playerId })
          .height(48)
          .enableKeyboardOnFocus(false)
          .onChange(value => this.playerId = value)
      }
      Button(this.register ? '注册并登录' : '登录')
        .width('100%')
        .height(48)
        .enabled(!this.busy && this.username.trim().length > 0 && this.password.length > 0 &&
          (!this.register || this.playerId.trim().length > 0))
        .onClick(() => void this.signIn())
      Button(this.register ? '返回登录' : '注册账户', { buttonStyle: ButtonStyleMode.TEXTUAL })
        .onClick(() => {
          this.register = !this.register;
          this.error = '';
        })
      if (this.error) {
        Text(this.error).fontColor($r('sys.color.font_secondary')).fontSize(14)
      }
    }
    .width('100%')
    .padding(20)
  }

  @Builder
  profileCard() {
    Column({ space: 14 }) {
      Row({ space: 12 }) {
        if (this.profile?.playerId) {
          Image(ORIGIN + '/api/player-avatar/' + encodeURIComponent(this.profile.playerId) + '?size=64')
            .width(48).height(48).borderRadius(24)
        }
        Column({ space: 4 }) {
          Text(this.profile?.username || '账户').fontSize(20).fontWeight(FontWeight.Medium)
          Text(this.profile?.playerId || '').fontSize(13).fontColor($r('sys.color.font_secondary'))
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
        Button('退出', { buttonStyle: ButtonStyleMode.TEXTUAL })
          .onClick(() => void this.signOut())
      }
      Row() {
        Column({ space: 5 }) {
          Text('当前余额').fontSize(13).fontColor($r('sys.color.font_secondary'))
          Text(this.economy?.balanceFormatted || money(this.economy?.balance))
            .fontSize(28).fontWeight(FontWeight.Bold)
        }.alignItems(HorizontalAlign.Start).layoutWeight(1)
        Column({ space: 5 }) {
          Text(this.economy?.role || '').fontSize(14)
          Text(this.playTimeLabel())
            .fontColor($r('sys.color.font_secondary')).fontSize(12)
        }.alignItems(HorizontalAlign.End)
      }
    }
    .padding(16)
    .borderRadius(20)
    .backgroundColor(SERVICE_CARD_COLOR)
  }

  @Builder
  ledgerHeader() {
    Column({ space: 8 }) {
      Row({ space: 6 }) {
        Text('零钱明细').fontSize(19).fontWeight(FontWeight.Medium).layoutWeight(1)
        Select([{ value: '排除飞行' }, { value: '全部' }, { value: '仅飞行' }])
          .selected(this.filterIndex)
          .value(['排除飞行', '全部', '仅飞行'][this.filterIndex])
          .onSelect(index => {
            this.filterIndex = index;
            this.filter = ['exclude_flight', 'all', 'flight'][index];
            this.ledger = null;
            void this.refresh();
          })
        Button('刷新', { buttonStyle: ButtonStyleMode.TEXTUAL })
          .enabled(!this.busy)
          .onClick(() => void this.refresh())
      }
      if (this.ledger?.summary) {
        Text('收入 ' + money(this.ledger.summary.creditTotal) + '  ·  支出 ' +
          money(this.ledger.summary.debitTotal) + '  ·  飞行 ' +
          money(this.ledger.summaryAll?.flightDebitTotal ?? this.ledger.summary.flightDebitTotal))
          .fontSize(13)
          .fontColor($r('sys.color.font_secondary'))
      }
    }.alignItems(HorizontalAlign.Start).width('100%')
  }

  build() {
    Column() {
      if (!this.token) {
        Scroll() {
          Column() {
            ScreenHeader({ title: '账户', caption: '使用流浪世界服务账户登录' })
            this.login()
          }.width('100%')
        }
          .layoutWeight(1)
          .width('100%')
          .contentEndOffset(BOTTOM_INSET)
          .clipContent(ContentClipMode.SAFE_AREA)
          .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
      } else {
        List({ space: 10 }) {
          ListItem() { ScreenHeader({ title: '账户' }) }
          ListItem() { this.profileCard() }
          ListItem() { this.ledgerHeader() }
          if (this.error || !this.ledger?.entries?.length) {
            ListItem() {
              LoadState({
                loading: this.busy,
                message: this.error || '暂无流水',
                actionLabel: this.error ? '重试' : '',
                onAction: () => void this.refresh()
              })
            }
          }
          ForEach(this.ledger?.entries || [], (entry: LedgerEntry) => {
            ListItem() {
              Row({ space: 12 }) {
                Column({ space: 5 }) {
                  Text(entry.description || entry.categoryLabel).fontSize(16).fontWeight(FontWeight.Medium)
                  Text(dateText(entry.createdAt))
                    .fontSize(12).fontColor($r('sys.color.font_secondary'))
                }.layoutWeight(1).alignItems(HorizontalAlign.Start)
                Column({ space: 5 }) {
                  Text(entry.signedAmountFormatted).fontSize(16)
                    .fontColor(entry.direction === 'credit' ? $r('sys.color.ohos_id_color_activated') :
                      $r('sys.color.font_primary'))
                  Text(entry.balanceAfterFormatted || '—').fontSize(12)
                    .fontColor($r('sys.color.font_secondary'))
                }.alignItems(HorizontalAlign.End)
              }.padding(14).borderRadius(16).backgroundColor(SERVICE_CARD_COLOR)
            }
          }, (entry: LedgerEntry) => entry.id)
        }
        .padding({ left: 16, right: 16 })
        .width('100%')
        .layoutWeight(1)
        .cachedCount(4)
        .contentEndOffset(BOTTOM_INSET)
        .clipContent(ContentClipMode.SAFE_AREA)
        .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
      }
    }
    .width('100%').height('100%')
    .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
  }
}
