package ru.david.room.client;

/**
 * Генератор случайных фраз
 */
public class WelcomePhrases {
    private static String[] phrases = {
            "Добро пожаловать, мой господин!",
            "Привет, салага! У меня есть годное дерьмо для тебя",
            "Сегодня отличная погода, не правда ли?",
            "Лишь глупцы называют своеволие свободой.\n  - Публий Корнелий Тацит",
            "— Ну, как посмотреть.\n" +
                    "— Всегда уместно сказать: «Как посмотреть». Конечно, как посмотреть.\n" +
                    "— Конечно, как посмотреть. А как же еще?\n" +
                    "— Не спорю, ты прав. Конечно, как посмотреть.\n" +
                    "(Из к/ф 'Отель «Гранд Будапешт»')",
            "Сразу ничего не дается. Чтобы удалось, надо пробовать и сегодня, и завтра, и послезавтра.\n  Евгений Шварц, \"Голый король\"",
            "Парадокс чтения: оно уводит нас от реальности, чтобы наполнить реальность смыслом.\n  Даниэль Пеннак, \"Как роман\"",
            "Живой интерес — вот великая движущая сила, единственная, которая ведет в правильном направлении и далеко.\n  Даниэль Пеннак, \"Как роман\"",
            "– Ты слишком много смеешься, это несерьезно.\n– А ты слишком серьезный, и это не смешно.\n  Даниэль Пеннак, \"Глаз волка\"",
            "Иногда надо рисковать. Особенно в тех случаях, когда уверен, что хуже уже не будет",
            "Голова и должна идти кругом, это ее основная обязанность",
            "Низкоуровневый язык — это когда требуется внимание к вещам, которые никак не связаны с программами на этом языке.",
            "Программирование на С похоже на быстрые танцы на только что отполированном полу людей с острыми бритвами в руках.",
            "Не волнуйтесь, если что-то не работает. Если бы всё работало, вас бы уволили.",
            "В хорошем дизайне добавление чего-то стоит дешевле, чем сама эта вещь.",
            "В теории, теория и практика неразделимы. На практике это не так.",
            "Perl — это тот язык, который одинаково выглядит как до, так и после RSA шифрования.",
            "Всегда пишите код так, будто сопровождать его будет склонный к насилию психопат, который знает, где вы живете.",
            "Люди, которые думают, что ненавидят компьютеры, на самом деле ненавидят плохих программистов.",
            "Если вы дадите человеку программу, то займете его на один день. Если вы научите человека программировать, то займете его на всю жизнь.",
            "Мы наблюдаем общество, которое все больше зависит от машин, но при этом использует их все неэффективнее.",
            "Программисты — не математики, как бы нам этого ни хотелось.",
            "Работает? Не трогай.",
            "Последние нововведения в C++ были созданы, чтобы исправить предыдущие нововведения.",
            "Java — это C++, из которого убрали все пистолеты, ножи и дубинки.",
            "Если бы в Java действительно работала сборка мусора, большинство программ бы удаляли сами себя при первом же запуске.",
            "Есть всего два типа языков программирования: те, на которые люди всё время ругаются, и те, которые никто не использует.",
            "Молодые специалисты не умеют работать, а опытные специалисты умеют не работать.",
            "Преждевременная оптимизация — корень всех зол.",
            "Почаще задавайте себе вопрос «Что мне скрыть?» и вы удивитесь, сколько проблем проектирования растает на ваших глазах.",
            "Чтобы написать чистый код, мы сначала пишем грязный код, а затем рефакторим его.",
            "Для каждой сложной задачи существует решение, которое является быстрым, простым и неправильным.",
            "Тестирование не позволяет обнаружить такие ошибки, как создание не того приложения.",
            "Ходить по воде и разрабатывать программы, следуя спецификации, очень просто… если они заморожены.",
            "Если вы считаете, что С++ труден, попытайтесь выучить английский.",
            "Ограничение возможностей языка с целью предотвращения программистских ошибок в лучшем случае опасно.",
            "Думаю, это будет новой фичей. Только не говорите никому, что она возникла случайно.",
            "Тяжело улучшать код, который до этого уже улучшали много раз.",
            "Лень — главное достоинство программиста.",
            "Легче изобрести будущее, чем предсказать его.",
            "Магия перестаёт существовать после того, как вы понимаете, как она работает.",
            "Способ использования интеллекта важнее, чем его уровень."
    };

    /**
     * @return Случайная фраза. Можно использовать во время запуска приложения.
     */
    public static String getRandom() {
        return phrases[(int)(Math.random()*phrases.length)];
    }
}
