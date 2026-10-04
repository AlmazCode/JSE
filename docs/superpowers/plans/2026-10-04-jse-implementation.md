# JSE Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking. Текущий план предназначен для самостоятельной реализации Sabirzhanov Emil, Baktiyarova Aruzhan и Roziyeva Yasmina; создание агентов или передача им студенческого авторства не требуется.

**Goal:** Реализовать Java Simple Engine, Arena Demo и Rendering Showcase с шестью работающими паттернами и сдать PDF + ZIP до конца 9-й учебной недели.

**Architecture:** Один Maven-проект. SwingHost обеспечивает окно и кадр, JseEngine — фиксированный цикл, Scene — границу движка и игры. World хранит объекты, фабрики создают продукты, AI задаёт скорости, правила применяют столкновения, Observer обновляет представление. Renderer и Graphic связывают две независимые оси Bridge; GraphicDecorator добавляет визуальные слои.

**Tech Stack:** Java 17, Swing, Java2D, Maven 3.9.9, Wrapper 3.3.2, Compiler 3.13.0, Surefire 3.2.5, Shade 3.5.3, JUnit 5.10.2.

**Spec:** [Готовое ТЗ версии 1.0](../../JSE-Technical-Specification.md). Раздел «Нормативные интерфейсы Java» определяет точные типы, сигнатуры и значения; при чтении задачи он обязателен. Относительные пути задач считаются от корня JSE.

## Global Constraints

- Sabirzhanov Emil реализует Bridge + Decorator; Baktiyarova Aruzhan — Factory Method + Abstract Factory; Roziyeva Yasmina — Strategy + Observer. Остальные части также имеют владельцев ниже.
- Production: Java 17, UTF-8, без дополнительных библиотек; test: JUnit 5.10.2.
- Один проект `edu.jse:jse-demo:1.0.0`, main class `jse.demo.DemoApplication`, JAR `target/jse-demo.jar`.
- Viewport 960×540; arena Rect(24,64,912,452); dt=1/60; максимум 5 update/pump; realDelta≤0.25.
- Player 24×24/HP3/speed220; Enemy 24×24/speed110; Pickup 16×16/value1; цель8; cooldown0.75.
- World≤128 с pending; Arena≤16 Enemy и ≤16 Pickup; один Player.
- Рисование и изменения симуляции на EDT; никакого Graphics2D вне кадра, загрузки файлов в paint и sleep в тестах.
- Два рабочих режима Filled/Wireframe, два семейства Forest/Space, три демонстрируемые стратегии Patrol/Chase/Flee.
- Новые функции прекращаются после недели7; неделя8 — кандидат на сдачу; неделя9 — резерв, упаковка и загрузка.
- E1 подготовлен и проверен на JDK 17/Maven 3.9.9. Текущий DemoApplication — временная консольная точка входа для проверки JAR. Остальные проверки ниже — инструкции исполнителю; игровая реализация и результаты её тестирования пока отсутствуют.

## Review Focus

1. Две физические клавиши одного действия: отпускание W при удерживаемой ↑ сохраняет MOVE_UP; тест E5.
2. Удаление ещё не добавленного объекта: ID расходуется, событие spawn/remove не появляется; тест Y2.
3. Listener создаёт подписку, закрывает другую и публикует событие во время dispatch: текущая доставка стабильна, новое событие отложено; тест Y2.
4. Одновременные последний Pickup и смертельный контакт: LOST, один GameFinished; тест Y4.
5. Переход сцены при накопленных шагах и удерживаемой клавише: старая сцена закрыта один раз, новая не получает старый pressed/held; тест E5 + Y5.

## Работа по задачам

На каждом шаге реализации используются локальные feature-ветки под настоящим авторством. Не требуется запрашивать решение по уже зафиксированным именам. Общие интерфейсы меняются только вместе с потребителями и ТЗ.

**Общий цикл задач с кодом:** написать указанные поведенческие тесты → запустить и увидеть отказ из-за отсутствующего поведения → реализовать → повторить команду и получить BUILD SUCCESS, tests failures=0/errors=0 → сделать связный коммит перечисленных файлов и тестов. Ошибка компиляции на стадии нового контракта допустима как начальный отказ теста; в main объединяется только собирающийся набор. Для документов и ресурсов достаточно их проверки, искусственные unit-тесты не нужны.

E2, A1 и Y1 образуют **один совместный набор базовых типов**, поскольку Enemy и BehaviorStrategy взаимно ссылаются друг на друга в Java. Каждый пишет свои файлы в своей ветке, затем три изменения объединяются в интеграционной ветке одним проверяемым набором. Отдельный неполный набор не объявляется готовым и не вливается в main. Конкретные фабрики, циклы и системы на этом этапе не нужны; заглушки рабочих методов и фиктивные реализационные паттерны не принимаются. Базовые value objects, абстрактные классы с общей логикой и интерфейсы являются окончательными контрактами.

| Этап | Задачи и владельцы | Зависимость | Завершение |
| --- | --- | --- | --- |
| Сборка | E1 — Sabirzhanov Emil | Нет | Начало недели5 |
| Базовые типы | E2 — Sabirzhanov Emil, A1 — Baktiyarova Aruzhan, Y1 — Roziyeva Yasmina | E1, совместный набор | Неделя5 |
| Параллельная реализация | E3/E4/E5; A2/A3; Y2/Y3/Y4/Y5 | По задачам ниже | Недели5–6 |
| Первая интеграция | I1 — все | E3, E5, A2, Y2, ранний Y5 | Конец недели5 |
| Полный продукт | E6 и завершение I1 | Все обязательные подсистемы | Конец недели7 |
| Материалы и кандидат | D1 — каждый; D2 — Sabirzhanov Emil, Baktiyarova Aruzhan, Roziyeva Yasmina | Полный продукт | Неделя8 |
| Сдача | D3 — Sabirzhanov Emil, Baktiyarova Aruzhan, Roziyeva Yasmina | D1/D2 | До конца недели9 |

### E1 — Sabirzhanov Emil — сборка и упаковка

Инфраструктура подготовлена в стартовом репозитории по запросу команды. Проверка: JDK 17.0.20.1, Maven 3.9.9, clean verify → BUILD SUCCESS; стартовый JAR выполняется. Поведенческих тестов движка пока нет. Коммит стартового репозитория включает также документацию, пакеты и GitHub-метаданные.

**Файлы:** создать `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, `.gitignore`, начальный `README.md`. Production-пакеты создаются следующими задачами.

**Interfaces:** производит воспроизводимую Maven-сборку и команды `./mvnw test`, `./mvnw clean verify`, `java -jar target/jse-demo.jar`; потребляет таблицу версий ТЗ.

- [x] Создать POM с зафиксированными координатами/версиями, release17, UTF-8, JUnit scope=test, Surefire и Shade package/ManifestResourceTransformer. Main-Class указывает временный DemoApplication; запуск игрового окна станет возможен в I1. Указанные Maven-плагины имеют groupId `org.apache.maven.plugins`.
- [x] Сгенерировать Wrapper командой `mvn -N org.apache.maven.plugins:maven-wrapper-plugin:3.3.2:wrapper -Dtype=only-script -Dmaven=3.9.9`. Системный Maven нужен только для генерации; далее используется Wrapper.
- [x] Выполнить `./mvnw --version` и `./mvnw clean verify`: Maven3.9.9, Java17, BUILD SUCCESS. Отсутствие тестов на этом этапе не доказывает готовность продукта.
- [x] Записать команды и требования Java в README; исключить target, .idea и локальные IDE-файлы из Git.
- [x] Коммит `build: configure reproducible Java 17 project`.

### E2 — Sabirzhanov Emil — значения и графические контракты

**Файлы:** создать `src/main/java/jse/math/{Vec2,Rect,Rgba}.java`, `render/{Renderer,Graphic,HealthView,RenderMode}.java`, `assets/SpriteId.java`, `input/{GameAction,InputState,InputSource}.java`.

**Tests:** `src/test/java/jse/math/GeometryTest.java`, `input/InputStateTest.java`. Фигурные скобки в путях здесь и далее обозначают отдельный файл для каждого имени.

**Interfaces:** производит конечные Vec2/Rect, защитные InputState, Graphic(Renderer)/draw(Rect)/setRenderer(Renderer) и интерфейс Renderer из ТЗ; потребители — A1/Y1, затем все графические и игровые задачи.

- [ ] Написать `zeroVectorNormalizesToZero`, `rejectsNonFiniteCoordinates`, `edgeTouchIsNotOverlap`, `inputSnapshotIsImmutable`: ZERO не даёт NaN; Vec2(NaN,0) отклонён; Rect(0,0,10,10) не пересекает Rect(10,0,10,10); изменение исходного Set не меняет InputState.
- [ ] Запустить `./mvnw -Dtest=GeometryTest,InputStateTest test`, подтвердить начальный отказ.
- [ ] Реализовать value objects и указанные интерфейсы; Graphic хранит protected Renderer и проверяет зависимости.
- [ ] Повторить команду, проверить общий набор E2/A1/Y1 через `./mvnw test`.
- [ ] Коммит `feat: define geometry input and graphics contracts`.

### A1 — Baktiyarova Aruzhan — категории и модель объектов

**Файлы:** создать `src/main/java/jse/world/{ObjectKind,ThemeId,GameObject,Player,Enemy,Pickup}.java`, `factory/SpawnRequest.java`.

**Tests:** `src/test/java/jse/world/GameObjectTest.java`; тестовые конкретные наследники объявить только внутри теста.

**Interfaces:** потребляет Graphic/HealthView/Vec2/Rect из E2 и BehaviorStrategy/IdleBehavior из Y1; производит конструкторы категорий, свойства объекта и операции World над ID/active согласно ТЗ.

- [ ] Написать `playerStartsWithThreeHealth`, `damageClampsToZero`, `idCanOnlyBeAssignedOnce`, `baseGraphicSurvivesDecoration`, `enemyStartsIdle`: health3/max3/speed220; damage10→0; второй assignId отклонён; setGraphic не меняет baseGraphic; Enemy speed110, Idle→ZERO, Pickup value1.
- [ ] Запустить `./mvnw -Dtest=GameObjectTest test`, подтвердить отказ в совместном наборе оснований.
- [ ] Реализовать классы без конкретной темы, с неизменными category/family/size/baseGraphic, velocity ZERO и состоянием id0/active=false. Валидация размера и health соответствует ТЗ.
- [ ] Повторить команду и общий `./mvnw test` после объединения E2/A1/Y1.
- [ ] Коммит `feat: add typed game object model`.

### Y1 — Roziyeva Yasmina — контракты AI и событий, снимки мира

**Файлы:** создать `src/main/java/jse/world/{EntityView,WorldView}.java`, `ai/{BehaviorStrategy,IdleBehavior}.java`, `event/{GameEvent,GameEventListener,Subscription,RemovalReason,GameResult,EntitySpawned,EntityRemoved,ItemCollected,HealthChanged,GameFinished,BehaviorChanged}.java`.

**Tests:** `src/test/java/jse/world/WorldViewTest.java`, `event/GameEventTest.java`.

**Interfaces:** потребляет категории A1 и math E2; производит неизменные снимки, точные event records с runId/tick и BehaviorStrategy.desiredVelocity(Enemy,WorldView,double):Vec2.

- [ ] Написать `snapshotDefensivelyCopiesEntities`, `eventsRejectInvalidIdentifiers`, `idleAlwaysReturnsZero`: очистка исходного списка не меняет WorldView; runId0/tick−1/entityId0 отклоняются; Idle возвращает ZERO при корректном Enemy/dt.
- [ ] Запустить `./mvnw -Dtest=WorldViewTest,GameEventTest test`, подтвердить отказ.
- [ ] Реализовать записи, защитные копии, интерфейсы и IdleBehavior; не хранить GameObject внутри EntityView.
- [ ] Повторить команду, объединить три части основы и выполнить `./mvnw clean verify`.
- [ ] Коммит `feat: define AI events and immutable world views`.

### E3 — Sabirzhanov Emil — Bridge и настоящие рендереры

**Зависимость:** совместный набор E2/A1/Y1.

**Файлы:** создать `src/main/java/jse/render/{SpriteGraphic,RectangleGraphic}.java`, `render/awt/{FrameRenderer,FilledRenderer,WireframeRenderer}.java`, `assets/AssetManager.java`, шесть PNG по путям ТЗ. Создать тестовый `src/test/java/jse/support/RecordingRenderer.java`.

**Tests:** `render/BridgeTest.java`, `render/RendererImageTest.java`, `assets/AssetManagerTest.java` в `src/test/java/jse/`.

**Interfaces:** потребляет Renderer/Graphic/SpriteId; производит SpriteGraphic(Renderer,SpriteId), RectangleGraphic(Renderer,Rgba), FrameRenderer.beginFrame/endFrame, AssetManager.preload/get; тестовый конструктор AssetManager(ClassLoader) доступен внутри jse.assets.

- [ ] Написать `bothAbstractionsDelegateToChosenRenderer`, `filledAndWireframeProduceDifferentImages`, `drawingOutsideFrameFails`, `assetsAreCachedAndMissingAssetsUsePlaceholder`: смена renderer перенаправляет вызовы; центральная область rectangle заполнена в Filled и не заполнена в Wireframe; draw вне кадра отклонён; два get одного ID возвращают один BufferedImage. Отсутствующий ресурс моделируется тестовым classpath-ресурсом/загрузчиком, не удалением production-PNG.
- [ ] Запустить `./mvnw -Dtest=BridgeTest,RendererImageTest,AssetManagerTest test`, подтвердить отказ.
- [ ] Реализовать оба реальных режима на BufferedImage/Graphics2D, текст/outline и classpath-кэш. Подготовить собственные PNG нужных размеров; отдельной библиотеки не добавлять.
- [ ] Повторить команду; сравнить существенные пиксельные области, не весь растр шрифта. Начальный Filled уже доступен для I1, Wireframe завершается к неделе6.
- [ ] Коммит `feat: implement Bridge rendering and sprite assets`.

### Y2 — Roziyeva Yasmina — EventBus и World

**Зависимость:** основания E2/A1/Y1.

**Файлы:** создать `src/main/java/jse/event/EventBus.java`, `world/World.java`, тестовый `src/test/java/jse/support/WorldFixtures.java` для создания объектов с RecordingRenderer и фиксации событий; использовать его в последующих тестах.

**Tests:** `src/test/java/jse/event/EventBusTest.java`, `world/WorldTest.java`.

**Interfaces:** производит EventBus.subscribe/publish/dispatchPending/close, World(runId,arena,family,events), enqueueAdd/remove/commitChanges/view/forEachActive/findActive/sizeIncludingPending.

- [ ] Написать `pendingAddIsInvisibleUntilCommit`, `idsStayUniqueAfterRemoval`, `capacityIncludesPending`, `cancelPendingAddEmitsNothing`, `removalImmediatelyExcludesObject`, `snapshotRemainsUnchanged`: ID1/2 назначены по очереди; 129-й объект отклонён; отменённое добавление не даёт spawn/remove; сохранённый WorldView не меняется.
- [ ] Написать `unsubscribeIsIdempotent`, `duplicateSubscriptionFails`, `subscriptionMutationDoesNotChangeCurrentDispatch`, `callbackPublicationWaitsForNextDispatch`, `closedBusRejectsWork`, `queueOverflowFails`: две действующие подписки получают событие; закрытая — нет; новая не получает текущий пакет; callback-событие приходит только при втором dispatch; 1025-й pending-event отклонён. Тип сравнивается точно.
- [ ] Запустить `./mvnw -Dtest=WorldTest,EventBusTest test`, подтвердить отказ.
- [ ] Реализовать stable-ID storage, очереди, снимки и EventBus по ТЗ; commit удаляет до добавления, а dispatch фиксирует все listener-списки в начале.
- [ ] Повторить команду, затем `./mvnw test`; коммит `feat: add queued world lifecycle and event dispatch`.

### A2 — Baktiyarova Aruzhan — Factory Method и Abstract Factory

**Зависимость:** A1, E3 (SpriteGraphic), Y2 (World). До готовности World реализовать AF и его тесты; FM-тесты завершить после Y2.

**Файлы:** создать `src/main/java/jse/factory/{GameObjectFamilyFactory,EntitySpawner,PlayerSpawner,EnemySpawner,PickupSpawner,ForestFactory,SpaceFactory}.java`, `theme/forest/{ForestPlayer,ForestEnemy,ForestPickup}.java`, `theme/space/{SpacePlayer,SpaceEnemy,SpacePickup}.java`.

**Tests:** `src/test/java/jse/factory/FamilyFactoryTest.java`, `SpawnerTest.java`.

**Interfaces:** потребляет модели A1/Renderer E2/World Y2; производит три final spawn(World,SpawnRequest) и две фабрики с familyId/createPlayer/createEnemy/createPickup. Точные конструкторы из ТЗ.

- [ ] Написать параметризованный `allSixCategoryFamilyCombinationsWork`: три spawner × две family factory, верные category/family, pending до commit, после commit ровно один EntitySpawned и active=true.
- [ ] Написать `rejectsWrongSizeAndOutsideArena`, `rejectsMixedFamily`, `familiesPreserveGameplayContracts`: Player25×24 и bounds вне арены отклоняются, Space-продукт не попадает в Forest World, оба Player HP3/speed220, оба Enemy speed110 и оба Pickup value1.
- [ ] Запустить `./mvnw -Dtest=FamilyFactoryTest,SpawnerTest test`, подтвердить отказ.
- [ ] Реализовать общий final spawn и protected переопределения, создание конкретных продуктов только в family factory. Не заменять наследников switch-фабрикой.
- [ ] Повторить команду; коммит `feat: implement object spawners and themed families`.

### A3 — Baktiyarova Aruzhan — воспроизводимое наполнение арены

**Зависимость:** A2, Y3 для конкретных начальных стратегий; таблицу SpawnRequest можно подготовить заранее.

**Файлы:** создать `src/main/java/jse/demo/ArenaLayout.java`; обновить `docs/uml/factory-patterns.puml`, `spawn-sequence.puml` по реализации.

**Tests:** `src/test/java/jse/demo/ArenaLayoutTest.java`.

**Interfaces:** потребляет три spawner и World; производит ArenaLayout.populate(World,GameObjectFamilyFactory):void. Метод ставит объекты в очередь; commit и dispatch выполняет вызывающая сцена.

- [ ] Написать `startsWithTwelveNonOverlappingObjectsInBothThemes`: после populate+commit ровно1 Player,3 Enemy,8 Pickup, все bounds внутри Rect(24,64,912,452), попарных overlap нет, позиции совпадают с таблицей ТЗ.
- [ ] Запустить `./mvnw -Dtest=ArenaLayoutTest test`, подтвердить отказ.
- [ ] Создать фиксированные SpawnRequest; назначить Patrol/Chase/Flee в определённом порядке; для Patrol точки (120,130)/(300,130). Ранний I1 использует один Player через тот же PlayerSpawner, затем заменяет наполнение на populate.
- [ ] Повторить команду и проверку двух тем; обновить UML без фиктивных классов.
- [ ] Коммит `feat: define deterministic arena layout`.

### E4 — Sabirzhanov Emil — Decorator и переключение слоёв

**Зависимость:** E3, A1 (HealthView у Player).

**Файлы:** создать `src/main/java/jse/render/{GraphicDecorator,OutlineDecorator,HealthBarDecorator}.java`, `demo/DemoPreferences.java`.

**Tests:** `src/test/java/jse/render/DecoratorTest.java`; повторные переключения сцены проверяются в I1.

**Interfaces:** потребляет Graphic/Renderer/HealthView; производит конструкторы декораторов и preferences с FOREST/FILLED/false/false/true.

- [ ] Написать `baseDrawsOnceWithBothLayers`, `rendererReachesNestedGraphic`, `healthBarUsesCurrentReadOnlyValues`: цепочка вызывает base ровно1 раз, outline и bar после базы, замена renderer достигает base, health2/3 даёт заполнение2/3, смена health меняет только bar.
- [ ] Запустить `./mvnw -Dtest=DecoratorTest test`, подтвердить отказ.
- [ ] Реализовать наследование GraphicDecorator, делегирование, толщину/позиции/цвета по ТЗ. Preferences хранит настройки; перестроение цепочек выполняется сценами, только от baseGraphic.
- [ ] Повторить команду, проверить четыре комбинации обёрток.
- [ ] Коммит `feat: add composable graphics decorators`.

### Y3 — Roziyeva Yasmina — Strategy

**Зависимость:** Y1/A1/Y2.

**Файлы:** создать `src/main/java/jse/ai/{PatrolBehavior,ChaseBehavior,FleeBehavior,AiSystem}.java`.

**Tests:** `src/test/java/jse/ai/BehaviorStrategyTest.java`, `AiSystemTest.java`.

**Interfaces:** потребляет Enemy/World/WorldView; производит три стратегии и AiSystem.update(World,WorldView,double):void, записывающий velocity без перемещения.

- [ ] Написать `chasePointsTowardPlayer`, `fleePointsAwayWithin180`, `zeroDistanceIsSafe`, `missingPlayerReturnsZero`, `patrolDoesNotOvershoot`, `swappingBehaviorKeepsIdentity`: Enemy(0,0) и Player(100,0) → Chase(110,0); Flee(−110,0); совпадение → Chase ZERO/Flee(110,0); дистанция181 → Flee ZERO; шаг к waypoint не превышает расстояние; id/pos при setBehavior прежние.
- [ ] Запустить `./mvnw -Dtest=BehaviorStrategyTest,AiSystemTest test`, подтвердить отказ.
- [ ] Реализовать алгоритмы по ТЗ, waypoint-состояние внутри Patrol; никаких instanceof/switch в AiSystem по конкретным стратегиям.
- [ ] Повторить команду; проверить, что AiSystem меняет только velocity, не position/score/health.
- [ ] Коммит `feat: implement swappable enemy strategies`.

### E5 — Sabirzhanov Emil — окно, ввод и игровой цикл

**Зависимость:** E2/E3/Y2; lifecycle-потребители Scene/SceneManager выполняются вместе с Y5. E4 нужен для shared preferences при полной интеграции.

**Файлы:** создать `src/main/java/jse/core/{EngineConfig,EngineHost,EngineControl,JseEngine}.java`, `platform/awt/{SwingHost,SwingInputSource,GamePanel}.java`; тестовые `src/test/java/jse/support/{FakeHost,FakeScene,FakeInputSource}.java`. Scene/SceneManager/EngineServices создаёт Roziyeva Yasmina в Y5.

**Tests:** `src/test/java/jse/core/EngineLoopTest.java`, `platform/awt/SwingInputSourceTest.java`. SwingInputSource предоставляет package-private `press(int keyCode)`, `release(int keyCode)` для bindings и тестов в пакете `jse.platform.awt`.

**Interfaces:** потребляет Scene lifecycle/InputSource/FrameRenderer; производит JseEngine(config,host,input), start/pump/render/requestScene/setRenderMode/stop и EngineHost callbacks из ТЗ. Согласованного изменения сигнатур не требуется.

- [ ] Написать `pumpRunsAtMostFiveSteps`, `longDeltaDropsWholeBacklog`, `pressedIsConsumedOnlyOnce`, `sceneChangeCancelsCatchUpAndClearsInput`: после первого pump0, pump1_000_000_000 даёт5 update, положительный droppedTime и remainder<1/60; pressed представлен только на первом update; переход отменяет остальные шаги и очищает ввод. Диагностику accumulator/droppedTime тестировать через доступный package-private снимок, без реального времени.
- [ ] Написать `releasingOnePhysicalKeyKeepsSharedActionHeld`, `repeatPressDoesNotRepeatCommand`, `focusLossClearsKeysAndPauses`, `closeIsIdempotent`, `failedUpdateShowsErrorWithoutClosingWindow`: последовательность W-down/↑-down/W-up сохраняет MOVE_UP; P-repeat даёт1 pressed; focusLost очищает оба Set и вызывает Scene.onFocusLost; два stop закрывают host/scene один раз; исключение из update (в том числе observer callback) даёт один showError, последующие pump не двигают мир, окно не закрывается до stop.
- [ ] Запустить `./mvnw -Dtest=EngineLoopTest,SwingInputSourceTest test`, подтвердить отказ. Тесты не создают настоящее окно; обработчики key press/release вызываются непосредственно на EDT.
- [ ] Реализовать цикл, bindings WHEN_IN_FOCUSED_WINDOW, отключение Tab traversal, GamePanel.paintComponent и finally для endFrame/dispose. Немонотонный nowNanos отклоняется. Ошибки callback останавливают симуляцию и вызывают host.showError.
- [ ] Повторить команду; вручную проверить размер content960×540, фокус и штатное закрытие. Коммит `feat: connect Swing input rendering and fixed update loop`.

### Y4 — Roziyeva Yasmina — движение, столкновения и правила

**Зависимость:** Y2/A1, конкретные стратегии Y3 только для сквозных проверок.

**Файлы:** создать `src/main/java/jse/physics/{MotionSystem,CollisionSystem,CollisionPair}.java`, `demo/{ArenaStatus,ArenaRules,SpawnLocator}.java`.

**Tests:** `src/test/java/jse/physics/{MotionSystemTest,CollisionSystemTest}.java`, `demo/{ArenaRulesTest,SpawnLocatorTest}.java`.

**Interfaces:** потребляет World snapshots и события; производит MotionSystem.update, CollisionSystem.findOverlaps, ArenaRules lifecycle/applyCollisions/evaluateOutcome и SpawnLocator.firstFree из ТЗ.

- [ ] Написать `motionUsesDtAndClampsArena`, `pairsAreUniqueAndEdgeTouchExcluded`, `pickupOnlyScoresOnce`, `contactDamageHas075Cooldown`, `lossWinsOverFinalPickup`, `finishedEventPublishedOnce`: velocity220 за0.5с даёт110px без выхода bounds; одна нормализованная пара; повторная пара Pickup не дублирует score; повторный contact раньше0.75с не уменьшает HP; HP1+score7+последний Pickup+Enemy→LOST и1 GameFinished.
- [ ] Написать `firstFreeSlotIncludesPendingAndRespectsLimits`: первый slot(48,88), после pending-объекта выбирается следующий свободный slot; превышение лимитов не ставит объект в World. Диагностические пределы категорий применяет ArenaScene, World проверяет только128.
- [ ] Запустить `./mvnw -Dtest=MotionSystemTest,CollisionSystemTest,ArenaRulesTest,SpawnLocatorTest test`, подтвердить отказ.
- [ ] Реализовать dt-движение, положительную площадь overlap, детерминированный порядок пар, сбор до урона и loss до win. SpawnLocator проверяет активные и pending-bounds через World.spawnView():WorldView; снимок не содержит mutable-ссылок.
- [ ] Повторить команду; коммит `feat: add arena movement collision and outcome rules`.

### Y5 — Roziyeva Yasmina — сцены и Observer в игре

**Зависимость:** Y2, ранний E5/A2/E3 для первого запуска; A3/Y3/Y4/E4 для полной Arena.

**Файлы:** создать `src/main/java/jse/core/{Scene,SceneManager,EngineServices}.java`, `demo/{TitleScene,ArenaScene,HudObserver,EventLogObserver}.java`. Соблюсти конструкторы ТЗ. ShowcaseScene создаётся E6, DemoApplication — I1.

**Tests:** `src/test/java/jse/core/SceneManagerTest.java`, `demo/{ObserversTest,ArenaSceneTest}.java`.

**Interfaces:** потребляет фабрики/World/системы/EngineControl; производит Scene.render(Renderer), lifecycle и сцены с preferences/runId. Художественное оформление HUD делает Sabirzhanov Emil, правила и подписки принадлежат Roziyeva Yasmina.

- [ ] Написать `exitAndEnterHappenExactlyOnce`, `latestRequestWins`, `twentyRestartsDoNotDuplicateSubscriptions`, `logCanUnsubscribeWhileHudUpdates`: события старой шины не попадают в новую; после20 перезапусков одно событие обновляет текущий HUD1 раз; close логгера не мешает HUD score1.
- [ ] Написать `diagonalPlayerVelocityHasLength220`, `pauseFreezesSimulationButCommandsStillWork`, `themeRestartChangesAllProductsAndPreservesVisualSettings`, `enemySwitchKeepsIdAndPosition`: MOVE_UP+MOVE_RIGHT даёт длину velocity220, а не220√2; пауза сохраняет position/HP/cooldown, F1/L работают; T создаёт Space run со score0/HP3, preferences прежние; B меняет strategy/событие без пересоздания.
- [ ] Запустить `./mvnw -Dtest=SceneManagerTest,ObserversTest,ArenaSceneTest test`, подтвердить отказ.
- [ ] Реализовать onEnter/Exit, fresh EventBus/World, event subscriptions, 10-шаговый update из ТЗ, приоритет команд перехода, Tab/B и spawn-диагностику. Для раннего I1 достаточно одного Player и motion через те же фабрики; к неделе6 заменить на полный ArenaLayout и системы. На каждом checkpoint main остаётся собирающимся.
- [ ] Повторить команду; коммит `feat: integrate arena scenes and event observers`.

### I1 — все — интеграция первой и полной игры

**Зависимость первого этапа:** E1–E3, E5, A1–A2, Y1–Y2 и ранний Y5. **Полный этап:** E4/A3/Y3/Y4/полныйY5.

**Файлы:** Sabirzhanov Emil создаёт `src/main/java/jse/demo/DemoApplication.java`, дополняет README запуском/управлением и тест `src/test/java/jse/demo/JseIntegrationTest.java`; владельцы дополняют свои классы при соединении. Roziyeva Yasmina отвечает за совместимость lifecycle, Baktiyarova Aruzhan — фабрик, Sabirzhanov Emil — графики/ввода.

**Interfaces:** composition root создаёт и связывает объекты ровно по разделу «Точки сборки приложения». Не заменяет существующие зависимости глобальными сервисами.

- [ ] Первый checkpoint: `./mvnw clean verify`, затем `java -jar target/jse-demo.jar`; окно960×540, один Player через PlayerSpawner + ForestFactory, движение/фокус/закрытие. Зафиксировать checkpoint к концу недели5.
- [ ] Написать `fullArenaUsesAllSixPatterns`, `repeatedDecoratorTogglesDoNotGrowChains`, `diagnosticLimitsRejectWithoutCrash`: после100 on/off base draw один раз; F1 сохраняет id/position; T меняет все family; B сохраняет id; ItemCollected обновляет HUD/log, после L только HUD; Enemy17/Pickup17 отклоняются без завершения сцены.
- [ ] Запустить `./mvnw -Dtest=JseIntegrationTest test`, подтвердить отказ до подключения возможностей.
- [ ] Подключить полную Arena, соблюдая update/commit/dispatch и role allocation; вручную показать шесть паттернов из одного JAR к концу недели6.
- [ ] Повторить тест и `./mvnw clean verify`; коммит `feat: integrate complete JSE arena demo`. Исправления собственных модулей коммитятся своими авторами.

### E6 — Sabirzhanov Emil — Rendering Showcase

**Зависимость:** E3/E4/E5/Y5.

**Файлы:** создать `src/main/java/jse/demo/ShowcaseScene.java`; связать TitleScene.H и Escape через уже заданные конструкторы, обновить README.

**Tests:** `src/test/java/jse/demo/ShowcaseSceneTest.java`.

**Interfaces:** производит обычную Scene с теми же Renderer/Graphic/Decorator, без World/ArenaRules.

- [ ] Написать `sameGraphicsWorkWithoutArenaRules`, `togglesKeepBaseGraphicsAndHealthTwoOfThree`, `escapeReturnsTitle`: оба типа Graphic рисуются, F1 меняет реализацию Renderer, F2/F3 дают четыре комбинации, фиксированный HealthView2/3, Escape ставит переход в очередь.
- [ ] Запустить `./mvnw -Dtest=ShowcaseSceneTest test`, подтвердить отказ.
- [ ] Реализовать два ряда демонстрационных графических компонентов. Размеры образцов40×40, колонки x=100/280/460/640, строки y=160/300; базовые sprite и rectangle, здоровье2/3. F2/F3 переключают обёртки у всех образцов, которые используют HealthView. Подписи читаемые и указывают тип/слои.
- [ ] Повторить команду и вручную проверить H/Escape, оба режима и композицию слоёв.
- [ ] Коммит `feat: add reusable rendering showcase scene`.

### D1 — каждый — UML, отчёт и доказательства

**Зависимость:** полная игра, окончательная структура классов.

**Файлы:** обновить шесть `docs/uml/*.puml` и `docs/uml/rendered/*`; создать `docs/screenshots/`, `docs/report/JSE_Report.md`, `docs/report/JSE_Report.pdf`, `docs/report/contributions.md`, `docs/report/resources.md`, `docs/report/verification.md`.

**Распределение:** Sabirzhanov Emil пишет Bridge/Decorator, графическую архитектуру, описание ввода/цикла и снимает скриншоты. Baktiyarova Aruzhan пишет обе фабрики, модель/наполнение и объединяет PDF. Roziyeva Yasmina пишет Strategy/Observer, World/сцены/физику/правила, integration и sequence diagrams. Каждый предоставляет SOLID-примеры и проверяемые ссылки на свой вклад.

- [ ] Обновить диаграммы по реальным классам и сигнатурам. Оставить шесть самостоятельных изображений паттернов для отчёта; объединённые диаграммы допускается разделить при экспорте. Диаграммы должны быть читаемы при вставке в PDF.
- [ ] Снять все перечисленные в ТЗ screenshots с окончательного JAR; дать каждому подпись о проверяемой возможности.
- [ ] Написать Introduction/Main Body/Conclusion/Further Work и остальные разделы по структуре ТЗ. Приложить полный листинг src/main/java; код Main Body показывает вызовы, а не только объявления интерфейсов.
- [ ] Записать среду, команды и действительные результаты проверок. Не писать «все тесты прошли» без итогового отчёта Surefire; не выдавать плановые показатели за измеренные.
- [ ] Экспортировать PDF, открыть и проверить все страницы: код, UML и подписи читаемы, таблица вклада соответствует Git. Коммит `docs: add final report diagrams and evidence`.

### D2 — все — кандидат на сдачу на неделе8

**Файлы:** создать `docs/report/manual-checks.md`, `release/JSE_Source.zip`, копию `release/JSE_Report.pdf`, `release/jse-demo.jar`. release не заменяет исходники в репозитории.

- [ ] Из свежей копии выполнить `./mvnw clean verify`, сохранить результаты и Java/Maven-версии в verification.md; исправить только обнаруженные проблемы и повторить затронутую проверку.
- [ ] Sabirzhanov Emil запускает JAR без IDE/сети на ноутбуке защиты. Все проходят десять ручных сценариев ТЗ; Roziyeva Yasmina проверяет restart/observer, Baktiyarova Aruzhan проверяет family/spawn.
- [ ] Выполнить десятиминутный прогон с лимитами16 Enemy/16 Pickup; измерить командную задержку≤250мс по записи, указать конфигурацию и фактические FPS/UPS/droppedTime. Результаты и найденные ограничения внести в отчёт.
- [ ] Sabirzhanov Emil упаковывает ZIP без .git/target/IDE-кэшей и исходных учебных PDF. Состав: src, pom, Wrapper, README, docs. Baktiyarova Aruzhan сверяет PDF; Roziyeva Yasmina распаковывает ZIP в отдельный каталог и выполняет сборку/запуск по README.
- [ ] Провести репетицию последовательности ТЗ, каждый защищает свои2 паттерна. Зафиксировать кандидат на сдачу и список только обязательных исправлений.

### D3 — Sabirzhanov Emil, Baktiyarova Aruzhan и Roziyeva Yasmina — сдача на неделе9

- [ ] Sabirzhanov Emil сверяет точную дату/время задания в Moodle. Срок «конец недели9» известен из силлабуса; календарная дата не назначается произвольно.
- [ ] Baktiyarova Aruzhan проверяет финальный PDF, Roziyeva Yasmina проверяет соответствие UML коду, Sabirzhanov Emil проверяет ZIP и готовый JAR; если были исправления, повторить соответствующие проверки D2.
- [ ] Загрузить `JSE_Report.pdf` и `JSE_Source.zip` в Moodle **до дедлайна**; проверить статус отправки, открыть/скачать вложения и убедиться, что это финальные файлы.
- [ ] Сохранить локальные копии PDF/ZIP/JAR и подтверждение отправки. Подготовить ноутбук защиты и резервные материалы для недели10.

## Покрытие требований задачами

| Требование | Задачи | Доказательство |
| --- | --- | --- |
| 2D engine, ввод, цикл, сцены | E2/E5/Y5/I1 | EngineLoopTest + работающий JAR |
| Bridge + Decorator — Sabirzhanov Emil | E3/E4/E6 | Реальные оба режима и комбинируемые слои |
| Factory Method + Abstract Factory — Baktiyarova Aruzhan | A1/A2/A3 | 6 сочетаний category×family и общий spawn |
| Strategy + Observer — Roziyeva Yasmina | Y1/Y2/Y3/Y5 | B/L, стратегия без смены ID, event callbacks |
| Игровой исход и управление | Y4/Y5/I1 | Правила, пауза, theme/restart, manual checks |
| Ресурсы/ошибки/лимиты | E3/E5/Y2/Y4/I1 | Placeholder, error panel, отклонение spawn |
| SOLID, UML, скриншоты, индивидуальный вклад | D1 | Содержимое PDF и история Git |
| PDF+полный исходный проект ZIP | D1/D2/D3 | Проверенная упаковка и вложения Moodle |
| Недели5–9 и защита10 | Checkpoints I1/D2/D3 | Показ команды и подтверждение сдачи |

План готов к исполнению. Завершённой считается задача с выполненными шагами и указанным доказательством, а не только с созданными файлами. E1 подготовлен; реализация продолжается с общего набора E2/A1/Y1; ожидание дополнительных решений по архитектуре не требуется.
