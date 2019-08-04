package com.mz.user.domain.aggregate;

import com.mz.reactivedemo.common.ValidateResult;
import com.mz.reactivedemo.common.api.events.Command;
import com.mz.reactivedemo.common.util.Try;
import com.mz.user.UserMapper;
import com.mz.user.domain.command.AddShortener;
import com.mz.user.domain.event.ShortenerAdded;
import com.mz.user.domain.event.UserCreated;
import com.mz.user.dto.ContactInfoDto;
import com.mz.user.dto.UserDto;
import com.mz.user.message.ContactInfoPayload;
import com.mz.user.message.command.CreateContactInfo;
import com.mz.user.message.command.CreateUser;
import com.mz.user.view.UserDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

/**
 * Created by zemi on 2019-01-16.
 */
class UserAggregateTest {

  static final String FIST_NAME = "FistName";
  static final String LAST_NAME = "LastName";
  static final Instant CREATED_AT = Instant.now();

  @Test
  void createUserTest() {
    CreateUser cmd1 = CreateUser
        .builder()
        .firstName("FirstName")
        .lastName("LastName")
        .build();
    createUserTest(cmd1);

    CreateUser cmd2 = CreateUser
        .builder()
        .firstName("FirstName")
        .lastName("LastName")
        .contactInformation(ContactInfoPayload
            .builder()
            .email("test@test")
            .phoneNumber("+421 901 000 000")
            .build())
        .build();
    createUserTest(cmd2);
  }

  private void createUserTest(Command cmd) {
    UserAggregate subject = UserAggregate.of(UUID.randomUUID().toString());
    Try<ValidateResult> result = subject.validate(cmd);
    Assertions.assertEquals(1, result.get().events().size());
    Assertions.assertTrue(result.get().events().stream().allMatch(e -> e instanceof UserCreated));
  }

  @Test
  void createUser_nullCommand() {
    Assertions.assertFalse(UserAggregate.of(UUID.randomUUID().toString()).validate(null).isFailure());
  }

  @Test
  void createContactInfo() {
    UserDocument userDocument = new UserDocument(UUID.randomUUID().toString(), FIST_NAME, LAST_NAME, 1L,
        CREATED_AT, null);
    UserDto userDto = UserMapper.mapToDto.apply(userDocument);
    UserAggregate subject = UserAggregate.of(userDto);
    Try<ValidateResult> result = subject.validate(CreateContactInfo.builder().email("test@test.com").build());
    result.get().events().forEach(subject::apply);
    UserDto userWithInfo = subject.state();
    Assertions.assertTrue(userWithInfo.contactInformation().get().email().get().equals("test@test.com"));
  }

  @Test
  void ofTest() {
    UserDocument userDocument = new UserDocument(UUID.randomUUID().toString(), FIST_NAME, LAST_NAME, 1L,
        CREATED_AT, null);
    UserDto userDto = UserMapper.mapToDto.apply(userDocument);
    UserAggregate subject = UserAggregate.of(userDto);
    Try<ValidateResult> contactInforesult =
        subject.validate(CreateContactInfo.builder().email("test@test.com").build());
    contactInforesult.toOptional().ifPresent(r -> r.events().forEach(subject::apply));

    UserDto result = subject.state();
    ContactInfoDto contactInfoState = result.contactInformation().get();


    Assertions.assertEquals(userDto.id(), userDocument.getId());
    Assertions.assertEquals(userDto.version(), userDocument.getVersion());
    Assertions.assertEquals(userDto.firstName(), userDocument.getFirstName());
    Assertions.assertEquals(userDto.lastName(), userDocument.getLastName());
    Assertions.assertEquals(userDto.createdAt(), userDocument.getCreatedAt());

    Assertions.assertEquals("test@test.com", contactInfoState.email().get());
  }

  @Test
  void addShortener() {
    String userId = UUID.randomUUID().toString();
    String shortenerId = UUID.randomUUID().toString();
    UserDto userDto = UserDto.builder()
        .createdAt(Instant.now())
        .id(userId)
        .lastName("LastName")
        .firstName("FirstName")
        .version(0L)
        .build();
    UserAggregate subject = UserAggregate.of(userDto);

    UserDto userDto1 = subject.state();
    Assertions.assertTrue(userDto1.shortenerIds().isEmpty());

    AddShortener addShortener = AddShortener.builder()
        .userId(userId)
        .shortenerId(shortenerId)
        .build();
    Try<ValidateResult> validateResult = subject.validate(addShortener);
    Assertions.assertEquals(1, validateResult.get().events().size());
    Assertions.assertTrue(validateResult.get().events().stream().allMatch(e -> e instanceof ShortenerAdded));
    validateResult.get().events().forEach(subject::apply);
    UserDto userDto2 = subject.state();
    Assertions.assertEquals(1L, (long) userDto2.version());
    Assertions.assertEquals(shortenerId, userDto2.shortenerIds().stream().findFirst().get());
  }

}
