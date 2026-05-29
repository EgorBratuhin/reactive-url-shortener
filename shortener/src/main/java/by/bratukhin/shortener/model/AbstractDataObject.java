package by.bratukhin.shortener.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import by.bratukhin.meta.GenerateFieldNames;

///
/// Base class for entities with a UUID identifier and automatic auditing.
///
/// Implements [Persistable] for correct "new/persisted" state management
/// in Spring Data R2DBC via the `isNew` field.
///
@GenerateFieldNames
public abstract class AbstractDataObject implements Persistable<UUID> {

    ///
    /// Unique identifier.
    ///
    @Id
    private UUID id;

    ///
    /// Indicates whether the entity is new (not yet persisted).
    /// Used by Spring Data R2DBC to determine whether to issue an INSERT or UPDATE.
    ///
    @Transient
    private boolean isNew = true;

    ///
    /// Record creation date and time. Populated automatically.
    ///
    @CreatedDate
    private Instant createdAt;

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractDataObject that = (AbstractDataObject) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
