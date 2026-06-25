package com.cryptoshield.cryptoshield.user.repository;

import com.cryptoshield.cryptoshield.user.dto.RedisToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisTokenRepository extends CrudRepository<RedisToken,String> {
    boolean existsById(String id);

    @Override
    void deleteById(String s);
}
