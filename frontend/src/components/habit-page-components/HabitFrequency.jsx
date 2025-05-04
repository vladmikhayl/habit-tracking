import React from "react";

const HabitFrequency = ({
  frequencyType,
  daysOfWeek,
  timesPerWeek,
  timesPerMonth,
}) => {
  const formatFrequency = () => {
    switch (frequencyType) {
      case "WEEKLY_ON_DAYS":
        if (daysOfWeek?.length > 0) {
          const dayNames = {
            MONDAY: "понедельник",
            TUESDAY: "вторник",
            WEDNESDAY: "среда",
            THURSDAY: "четверг",
            FRIDAY: "пятница",
            SATURDAY: "суббота",
            SUNDAY: "воскресенье",
          };
          const dayOrder = [
            "MONDAY",
            "TUESDAY",
            "WEDNESDAY",
            "THURSDAY",
            "FRIDAY",
            "SATURDAY",
            "SUNDAY",
          ];
          const sortedDays = [...daysOfWeek].sort(
            (a, b) => dayOrder.indexOf(a) - dayOrder.indexOf(b)
          );
          return sortedDays.map((day) => dayNames[day]).join(", ");
        }
        return "Нет указанных дней";
      case "WEEKLY_X_TIMES":
        return `${timesPerWeek} раз(а) в неделю`;
      case "MONTHLY_X_TIMES":
        return `${timesPerMonth} раз(а) в месяц`;
      default:
        return "Неизвестно";
    }
  };

  return (
    <div>
      <span className="text-gray-500">
        {frequencyType === "WEEKLY_ON_DAYS"
          ? "Запланированные дни выполнения:"
          : "Частота выполнения:"}
      </span>
      <div className="text-base text-gray-800">{formatFrequency()}</div>
    </div>
  );
};

export default HabitFrequency;
