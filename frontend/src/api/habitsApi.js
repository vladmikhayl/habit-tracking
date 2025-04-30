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
};

export default habitsApi;
