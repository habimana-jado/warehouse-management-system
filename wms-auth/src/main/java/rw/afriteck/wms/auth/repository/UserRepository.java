package rw.afriteck.wms.auth.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.auth.entity.User;
import rw.afriteck.wms.auth.enums.RoleName;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("""
           select u from User u
           where :q is null
              or lower(u.username) like lower(concat('%', :q, '%'))
              or lower(u.fullName) like lower(concat('%', :q, '%'))
           """)
    Page<User> search(@Param("q") String q, Pageable pageable);

    @Query("select count(u) from User u join u.roles r where r = :role and u.active = true")
    long countActiveWithRole(@Param("role") RoleName role);
}
