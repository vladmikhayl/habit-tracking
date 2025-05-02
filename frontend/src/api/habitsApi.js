const habitsApi = {
  // Список всех текущих привычек юзера за конкретную дату
  getAllUserHabitsAtDay: async (date) => {
    const token = localStorage.getItem("token");

    const response = await fetch(`/api/habits/all-user-habits/at-day/${date}`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      throw new Error("Не удалось получить список привычек");
    }

    return await response.json();
  },

  // Список всех текущих привычек за конкретную дату, на которые подписан юзер
  getAllUserSubscribedHabitsAtDay: async (date) => {
    const token = localStorage.getItem("token");

    const response = await fetch(
      `/api/habits/all-user-subscribed-habits/at-day/${date}`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      throw new Error("Не удалось получить список привычек");
    }

    return await response.json();
  },

  // Создать привычку
  create: async ({
    name,
    description,
    isPhotoAllowed,
    isHarmful,
    durationDays,
    frequencyType,
    daysOfWeek,
    timesPerWeek,
    timesPerMonth,
  }) => {
    const token = localStorage.getItem("token");

    const response = await fetch(`/api/habits/create`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        name,
        description,
        isPhotoAllowed,
        isHarmful,
        durationDays,
        frequencyType,
        daysOfWeek,
        timesPerWeek,
        timesPerMonth,
      }),
    });

    if (!response.ok) {
      let data = null;
      try {
        data = await response.json();
      } catch (error) {}
      let errorMessage = "Не удалось создать привычку";
      if (data?.error) {
        errorMessage = data.error;
      } else if (Array.isArray(data?.errors)) {
        errorMessage = data.errors[0];
      }
      throw new Error(errorMessage);
    }
  },

  // Удалить привычку
  delete: async (habitId) => {
    const token = localStorage.getItem("token");

    const response = await fetch(`/api/habits/${habitId}/delete`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      throw new Error("Не удалось удалить привычку");
    }
  },

  // Отчет о привычке за конкретный день
  getReportAtDay: async (habitId, date) => {
    const token = localStorage.getItem("token");

    const response = await fetch(
      `/api/habits/${habitId}/get-report/at-day/${date}`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      throw new Error("Не удалось получить отчет");
    }

    return await response.json();
  },

  // Общая инфа о конкретной привычке
  getGeneralInfo: async (habitId) => {
    const token = localStorage.getItem("token");

    const response = await fetch(`/api/habits/${habitId}/general-info`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      throw new Error("Не удалось получить информацию о привычке");
    }

    return await response.json();
  },

  // Инфа по отчетам о конкретной привычке
  getReportsInfo: async (habitId) => {
    const token = localStorage.getItem("token");

    const response = await fetch(`/api/habits/${habitId}/reports-info`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      throw new Error("Не удалось получить информацию о привычке");
    }

    return await response.json();
  },
};

export default habitsApi;
