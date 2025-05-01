const subscriptionsApi = {
  // Список принятых подписчиков на привычку
  getHabitAcceptedSubscriptions: async (habitId) => {
    const token = localStorage.getItem("token");

    const response = await fetch(
      `/api/subscriptions/${habitId}/get-habit-accepted-subscriptions`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      throw new Error("Не удалось получить список подписчиков");
    }

    return await response.json();
  },

  // Список необработанные заявок на подписку на привычку
  getHabitUnprocessedRequests: async (habitId) => {
    const token = localStorage.getItem("token");

    const response = await fetch(
      `/api/subscriptions/${habitId}/get-habit-unprocessed-requests`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      throw new Error("Не удалось получить список подписчиков");
    }

    return await response.json();
  },

  // Принять заявку
  acceptSubscriptionRequest: async (subscriptionId) => {
    const token = localStorage.getItem("token");

    const response = await fetch(
      `/api/subscriptions/${subscriptionId}/accept`,
      {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      throw new Error("Не удалось принять заявку");
    }
  },

  // Отклонить заявку
  denySubscriptionRequest: async (subscriptionId) => {
    const token = localStorage.getItem("token");

    const response = await fetch(`/api/subscriptions/${subscriptionId}/deny`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      throw new Error("Не удалось отклонить заявку");
    }
  },
};

export default subscriptionsApi;
