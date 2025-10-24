package io.github.srushti1125.aggregator.repository;

import io.github.srushti1125.aggregator.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}