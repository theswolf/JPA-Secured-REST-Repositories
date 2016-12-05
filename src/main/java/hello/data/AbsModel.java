package hello.data;


import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import javax.annotation.PreDestroy;
import javax.persistence.*;
import javax.validation.ValidationException;

/**
 * Created by christian on 21/11/16.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AbsModel<T extends PagingAndSortingRepository> extends AbstractPersistable<Long>{


    @CreatedBy
    private String owner;

    @LastModifiedBy
    private String modifier;

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long modifiedAt;

    @Transient
    private String storedOwner;



    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        //this.owner =  owner != null ? owner : this.owner;

        this.owner = owner;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Long modifiedAt) {
        this.modifiedAt = modifiedAt;
    }



    @PostLoad
    public void onPostLoad() {
        this.storedOwner = this.owner;
    }



    @PreUpdate
    public void onPreUpdate() {
        if(!this.storedOwner.equals(this.getModifier())) {
            throw new ValidationException("Trying to modify a data not mine");
        }
    }

    //getCurrentAuditor is not called for delete
    @PreRemove
    public void onPreRemove() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !this.owner.equalsIgnoreCase(((User) authentication.getPrincipal()).getUsername())  ) {
            throw new ValidationException("Trying to delete a data not mine");
        }
    }


}
