package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.dto.TopTokenDTO;
import com.t1908e.memeportalapi.dto.UserDTO;
import com.t1908e.memeportalapi.entity.*;
import com.t1908e.memeportalapi.repository.*;
import com.t1908e.memeportalapi.specification.UserSpecificationBuilder;
import com.t1908e.memeportalapi.util.ConvertUtil;
import com.t1908e.memeportalapi.util.RESTResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;
    private final PostRepository postRepository;
    private final RoleRepository roleRepository;
    private final CommentRepository commentRepository;

    public ResponseEntity<?> getUserDetail(long userId) {
        HashMap<String, Object> restResponse = new HashMap<>();
        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.NOT_FOUND.value())
                    .setMessage("user not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        User user = userOptional.get();
        if (user.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("user is de-active").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        restResponse = new RESTResponse.Success()
                .setMessage("OK")
                .setStatus(HttpStatus.OK.value())
                .setData(new UserDTO(user)).build();

        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> updateUser(long id, UserDTO.UpdateUserProfileDTO updateDTO, String editorUsername) {
        HashMap<String, Object> restResponse = new HashMap<>();
        Optional<User> userOptional = userRepository.findById(id);
        if (!userOptional.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.NOT_FOUND.value())
                    .setMessage("user not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        User toUpdateUser = userOptional.get();
        if (toUpdateUser.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("user is de-active").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        //check editor
        User editor = authenticationService.getAppUser(editorUsername);
        if (!editor.getAccount().getRole().getName().equals("admin") && editor.getId() != toUpdateUser.getId()) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Permission denied").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        //update
        try {
            toUpdateUser.setAvatar(updateDTO.getAvatar());
            toUpdateUser.setPhone(updateDTO.getPhone());
            toUpdateUser.setFullName(updateDTO.getFullName());
            toUpdateUser.setBirthDay(ConvertUtil.convertStringToJavaDate(updateDTO.getBirthday(),
                    UserDTO.UpdateUserProfileDTO.DATE_FORMAT));
            toUpdateUser.setGender(updateDTO.getGender());
            toUpdateUser.setUpdatedAt(new Date());
            //save
            User saved = userRepository.save(toUpdateUser);
            restResponse = new RESTResponse.Success()
                    .setMessage("Success")
                    .setStatus(HttpStatus.OK.value())
                    .setData(new UserDTO(saved))
                    .build();
            return ResponseEntity.ok().body(restResponse);
        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .setMessage(exception.getMessage()).build();
            return ResponseEntity.internalServerError().body(restResponse);
        }

    }

    public ResponseEntity<?> deActiveUser(long userId) {
        HashMap<String, Object> restResponse = new HashMap<>();
        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.NOT_FOUND.value())
                    .setMessage("user not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        User toDeActive = userOptional.get();
        if (toDeActive.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("This user is already blocked").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        //can not de active an admin account
        if (toDeActive.getAccount().getRole().getName().equals("admin")) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Permission denied").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try {
            toDeActive.setStatus(-1);
            userRepository.save(toDeActive);
            postRepository.changePostStatusAccordingUserId(toDeActive.getId(), -1); //-1 DEACTIVE
            restResponse = new RESTResponse.Success()
                    .setMessage("Success")
                    .setStatus(HttpStatus.OK.value())
                    .build();
            return ResponseEntity.ok().body(restResponse);
        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .setMessage(exception.getMessage()).build();
            return ResponseEntity.internalServerError().body(restResponse);
        }

    }

    public ResponseEntity<?> searchListUsers(
            HashMap<String, Object> params,
            Integer page,
            Integer limit,
            String sortBy,
            String order
    ) {
        HashMap<String, Object> restResponse = new HashMap<>();
        UserSpecificationBuilder builder = new UserSpecificationBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.equals("role")) {
                continue;
            }
            builder.with(key, ":", value);
        }
        Specification<User> spec = builder.build();
        if (params.containsKey("role")) {
            List<Long> listIds = getListAccountIdByRole(params.get("role").toString());
            Specification<User> roleSpec = accountIdsIn(listIds);
            if (spec == null) {
                spec = roleSpec;
            } else {
                spec = spec.and(roleSpec);
            }
        }
        Sort.Direction direction;
        if (order == null) {
            direction = Sort.Direction.DESC;
        } else if (order.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        } else {
            direction = Sort.Direction.DESC;
        }
        Pageable pageInfo = PageRequest.of(page, limit, Sort.by(direction, sortBy));
        Page<User> all = userRepository.findAll(spec, pageInfo);
        Page<UserDTO> dtoPage = all.map(new Function<User, UserDTO>() {
            @Override
            public UserDTO apply(User user) {
                return new UserDTO(user);
            }
        });
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(dtoPage).build();
        return ResponseEntity.ok().body(restResponse);
    }

    private List<Long> getListAccountIdByRole(String role) {
        Optional<Role> roleOptional = roleRepository.findByName(role);
        if (!roleOptional.isPresent()) {
            return null;
        }
        Role inDbRole = roleOptional.get();
        Set<Account> accounts = inDbRole.getAccounts();
        return accounts.stream().map(Account::getId).collect(Collectors.toList());
    }

    private Specification<User> accountIdsIn(List<Long> types) {
        return (root, query, cb) -> {
            if (types != null && !types.isEmpty()) {
                return root.<String>get("accountId").in(types);
            } else {
                // always-true predicate, means that no filtering would be applied
                return cb.and();
            }
        };
    }

    public ResponseEntity<?> getTopTokenOwner() {
        Pageable topFive = PageRequest.of(0, 5);
        List<User> topToken = userRepository
                .findByStatusGreaterThanAndTokenBalanceGreaterThanOrderByTokenBalanceDesc(0, 0, topFive);
        List<TopTokenDTO> topTokenDTOList = new ArrayList<>();
        for (User topUser : topToken) {
            TopTokenDTO topTokenDTO = new TopTokenDTO(new UserDTO(topUser), topUser.getTokenBalance());
            topTokenDTOList.add(topTokenDTO);
        }
        return ResponseEntity.ok().body(new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(topTokenDTOList).build());
    }

    public ResponseEntity<?> getPostCreated(long userid) {
        HashMap<String, Object> restResponse = new HashMap<>();
        User user = userRepository.findById(userid).orElse(null);
        if (user == null || user.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("User not found or has been de-activated").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        int count = postRepository.countByUserIdAndStatusGreaterThan(user.getId(), 0);
        restResponse = new RESTResponse.Success()
                .setMessage("OK")
                .setStatus(HttpStatus.OK.value())
                .setData(count).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> getTotalComment(long userId) {
        HashMap<String, Object> restResponse = new HashMap<>();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("User not found or has been de-activated").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        int count = commentRepository.countByUserIdAndStatusGreaterThan(userId, 0);
        restResponse = new RESTResponse.Success()
                .setMessage("OK")
                .setStatus(HttpStatus.OK.value())
                .setData(count).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> getUserProfile(String username) {
        HashMap<String, Object> restResponse = new HashMap<>();
        User user = authenticationService.getAppUser(username);
        if (user == null) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.NOT_FOUND.value())
                    .setMessage("user not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        if (user.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("user is de-active").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        restResponse = new RESTResponse.Success()
                .setMessage("OK")
                .setStatus(HttpStatus.OK.value())
                .setData(new UserDTO(user)).build();

        return ResponseEntity.ok().body(restResponse);
    }
}
