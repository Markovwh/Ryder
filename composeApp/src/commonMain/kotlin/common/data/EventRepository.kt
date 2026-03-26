package common.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import common.model.Event
import kotlinx.coroutines.tasks.await

class EventRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val eventsRef = firestore.collection("events")

    suspend fun createEvent(event: Event): Event {
        val doc = eventsRef.document()
        val newEvent = event.copy(id = doc.id, createdAt = System.currentTimeMillis())
        doc.set(newEvent).await()
        return newEvent
    }

    suspend fun getEvents(): List<Event> =
        eventsRef.orderBy("dateTime").get().await().toObjects(Event::class.java)

    suspend fun getEventsForUser(userId: String): List<Event> =
        eventsRef.whereArrayContains("attendeeIds", userId).get().await().toObjects(Event::class.java)

    suspend fun getEvent(eventId: String): Event? =
        eventsRef.document(eventId).get().await().toObject(Event::class.java)

    suspend fun attendEvent(eventId: String, userId: String) {
        eventsRef.document(eventId).update("attendeeIds", FieldValue.arrayUnion(userId)).await()
    }

    suspend fun unattendEvent(eventId: String, userId: String) {
        eventsRef.document(eventId).update("attendeeIds", FieldValue.arrayRemove(userId)).await()
    }

    suspend fun updateEvent(event: Event) {
        eventsRef.document(event.id).set(event).await()
    }

    suspend fun deleteEvent(eventId: String) {
        eventsRef.document(eventId).delete().await()
    }

    suspend fun searchEvents(query: String): List<Event> {
        if (query.isBlank()) return emptyList()
        val snap = eventsRef.get().await()
        return snap.toObjects(Event::class.java)
            .filter {
                it.name.contains(query, ignoreCase = true) ||
                it.place.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
            .take(20)
    }
}
