package com.nowcoder.dao;

import com.nowcoder.model.User;
import org.apache.ibatis.annotations.*;


@Mapper
public interface UserDAO {
    String TABLE_NAME = "user";
    String INSET_FIELDS = " name, password, salt, head_url, active, code, email";
    String SELECT_FIELDS = " id, name, password, salt, head_url, active, code, email";

    @Insert({"insert into ", TABLE_NAME, "(", INSET_FIELDS,
            ") values (#{name},#{password},#{salt},#{headUrl},#{active},#{code},#{email})"})
    int addUser(User user);

    @Select({"select ", SELECT_FIELDS, " from ", TABLE_NAME, " where id=#{id}"})
    User selectById(int id);

    @Select({"select ", SELECT_FIELDS, " from ", TABLE_NAME, " where name=#{name}"})
    User selectByName(String name);

    @Select({"select ", SELECT_FIELDS, " from ", TABLE_NAME, " where email=#{email}"})
    User selectByEmail(String email);

    @Update({"update ", TABLE_NAME, " set password=#{password} where email=#{email}"})
    void updatePassword(@Param("email") String email, @Param("password") String password);

    @Update({"update ", TABLE_NAME, " set active=#{active} where id=#{id}"})
    void updateActive(@Param("id") int id, @Param("active") int active);

    @Delete({"delete from ", TABLE_NAME, " where id=#{id}"})
    void deleteById(int id);
}
