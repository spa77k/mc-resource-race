# ResourceRace

[English](README.md) | [日本語](README.ja.md) | [简体中文](README.zh-CN.md) | 한국어

자원 월드를 초기화한 직후부터, 그 월드에서 지정된 아이템을 모은 순서대로 순위를 정하고 스파코인을 지급하는 Paper 플러그인입니다.

- 레이스는 한 번에 하나만 진행됩니다. 새 레이스를 시작하면 진행 중인 레이스를 먼저 종료합니다.
- 목표 아이템은 월드마다 설정한 후보 중에서 하나를 추첨합니다.
- 보상은 순위마다 Vault를 통해 한 번만 지급합니다. 지급 여부를 저장하므로, 서버를 재시작해도 같은 사람에게 두 번 지급하지 않습니다.
- 순위는 채팅으로 전체에 알리고, 본인에게는 타이틀도 표시합니다.
- 레이스 중 대상 월드 안에서 얻은 아이템에만 표시를 붙이고, 그 아이템을 셉니다.
- 시작과 종료는 콘솔과 RCON에서만 할 수 있습니다. 플레이어는 `/race`로 진행 상황을 볼 수 있습니다.

## 요구 사항

- Paper 1.21.4 이상 (Paper 26.2에서 동작 확인)
- Java 21 이상 (서버 버전이 요구하는 버전)
- [Vault](https://www.spigotmc.org/resources/vault.34315/)와 경제 플러그인 (EssentialsX를 가정)

## 설치

1. [Releases](https://github.com/spa77k/ResourceRace/releases)에서 `resourcerace-1.0.0.jar`를 내려받습니다.
2. Vault와 같은 `plugins/` 폴더에 넣습니다.
3. 서버를 재시작합니다. `plugins/ResourceRace/config.yml`이 생성됩니다.
4. 자원 월드를 `worlds`에 적고, 서버를 재시작합니다.
5. 초기화 후 콘솔이나 RCON에서 `race start <월드>`를 실행합니다.

## 명령어

| 명령어 | 사용 가능 | 내용 |
| --- | --- | --- |
| `/race` | 모두 | 목표 아이템, 대상 월드, 마감 시각, 자신의 진행 상황, 순위표를 표시 |
| `/race start <월드>` | `resourcerace.admin` | 레이스를 시작. 진행 중인 레이스가 있으면 먼저 종료 |
| `/race stop` | `resourcerace.admin` | 진행 중인 레이스를 종료 |
| `/race reload` | `resourcerace.admin` | `config.yml`을 다시 불러옴 |

`resourcerace.admin`의 기본값은 OP입니다. 명령어는 콘솔과 RCON에서 사용할 수 있으며, 대상 월드가 아직 로드되지 않아도 됩니다.

### 반환 결과

| 결과 | 의미 |
| --- | --- |
| `RESOURCE_RACE_OK started world=<월드> item=<MATERIAL> amount=<개수> race=<race-id>` | 레이스를 시작함 |
| `RESOURCE_RACE_ERROR world-not-configured` | `worlds`에 없는 월드 |
| `RESOURCE_RACE_ERROR no-targets` | 그 월드의 후보가 모두 잘못됨 |
| `RESOURCE_RACE_OK stopped` | 레이스를 종료함 |
| `RESOURCE_RACE_ERROR no-race` | 종료할 레이스가 없음 |
| `RESOURCE_RACE_OK reloaded` | `config.yml`을 다시 불러옴 |

## 설정

`plugins/ResourceRace/config.yml`

| 키 | 기본값 | 내용 |
| --- | --- | --- |
| `worlds` | 자원 월드 3개 | 레이스를 할 수 있는 월드. 월드마다 `display-name`과 `targets`를 가집니다. |
| `worlds.<월드>.targets[].item` | | 후보의 Material 이름. |
| `worlds.<월드>.targets[].amount` | | 모아야 하는 개수. |
| `rewards` | `[1000, 500, 300]` | 순위별 스파코인. 요소 개수가 순위의 수가 됩니다. |
| `duration-hours` | `167` | 레이스 길이(시간). 다음 초기화와 겹치지 않도록 7일보다 조금 짧게 설정합니다. |

레이스를 시작하면 그 월드의 `targets`에서 하나를 추첨합니다. 알 수 없는 Material 이름이나 `amount`가 1 미만인 후보는 불러올 때 경고하고 제외합니다.

레이스와 순위는 `plugins/ResourceRace/state.yml`에 저장합니다.

## 표시가 붙는 방식

레이스 중 대상 월드 안에서 아이템을 얻으면 표시를 붙입니다. 목표 Material이면서, 현재 레이스의, 자신의 표시가 붙은 아이템만 셉니다. 인벤토리(보관 칸)와 보조 손은 세고, 엔더 상자는 세지 않습니다.

세는 것:

- 직접 부순 블록의 드롭 (컨테이너와 직접 설치한 블록은 제외).
- 몹 등 플레이어가 아닌 엔티티의 드롭.
- 상자 등 컨테이너에 생성된 전리품.
- 낚시로 낚은 아이템.
- 피글린과의 물물교환 결과.
- 자연 생성된 아이템 액자에서 꺼내거나, 부숴서 떨어진 아이템.
- 표시가 붙은 재료를 화로에서 구운 결과.
- 비어 있지 않은 재료가 모두 같은 주인의 표시를 가졌을 때의 제작 결과.

화로와 제작대는 재료의 표시를 이어받을 뿐이므로, 대상 월드 밖에서 사용해도 결과에 표시가 붙습니다. 순위 판정은 어느 월드에 있든 이루어집니다.

세지 않는 것:

- 대상 월드 밖에서 얻은 것.
- 직접 설치한 블록의 드롭.
- 부순 컨테이너의 드롭.
- 아이템을 주운 적이 있는 몹의 드롭.
- 직접 설치했거나 아이템을 넣은 아이템 액자의 아이템.
- 표시가 없는 것, 다른 사람의 표시가 붙은 것, 이전 레이스의 표시가 붙은 것.
- 엔더 상자 안의 것.

이전 레이스의 표시는 로그인할 때, 인벤토리를 열 때, 레이스가 끝날 때 지웁니다. 따라서 레이스가 끝난 뒤에는 일반 아이템과 다시 겹쳐 쌓을 수 있습니다.

## 라이선스

[MIT](LICENSE)
