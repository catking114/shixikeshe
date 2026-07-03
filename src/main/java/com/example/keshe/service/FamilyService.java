package com.example.keshe.service;

import com.example.keshe.entity.Family;
import com.example.keshe.entity.User;
import com.example.keshe.repository.FamilyRepository;
import com.example.keshe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;

    @Autowired
    public FamilyService(FamilyRepository familyRepository, UserRepository userRepository) {
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
    }

    private String generateFamilyCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (familyRepository.existsByFamilyCode(code));
        return code;
    }

    public Family createFamily(Long creatorId, String creatorRole, String familyName) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!"ADMIN".equals(creator.getRole())) {
            throw new RuntimeException("仅管理员可以创建家庭");
        }

        if (creator.getFamilyId() != null) {
            throw new RuntimeException("您已属于一个家庭，不能重复创建");
        }

        Family family = new Family();
        family.setFamilyCode(generateFamilyCode());
        family.setFamilyName(familyName != null ? familyName : creator.getUsername() + "的家庭");
        family.setCreatorId(creatorId);
        family.setCreatorRole(creatorRole);
        family.setCreateTime(LocalDateTime.now());
        family = familyRepository.save(family);

        creator.setFamilyId(family.getId());
        creator.setFamilyRole(creatorRole);
        userRepository.save(creator);

        return family;
    }

    public Family joinFamily(Long userId, String familyCode, String familyRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getFamilyId() != null) {
            throw new RuntimeException("您已属于一个家庭，不能重复加入");
        }

        Family family = familyRepository.findByFamilyCode(familyCode)
                .orElseThrow(() -> new RuntimeException("家庭码不存在: " + familyCode));

        user.setFamilyId(family.getId());
        user.setFamilyRole(familyRole);
        userRepository.save(user);

        return family;
    }

    public Family getFamilyById(Long familyId) {
        return familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("家庭不存在"));
    }

    public Family getFamilyByCode(String familyCode) {
        return familyRepository.findByFamilyCode(familyCode)
                .orElseThrow(() -> new RuntimeException("家庭码不存在"));
    }

    public List<User> getFamilyMembers(Long familyId) {
        return userRepository.findByFamilyId(familyId);
    }

    public void leaveFamily(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getFamilyId() == null) {
            throw new RuntimeException("您不属于任何家庭");
        }

        Family family = familyRepository.findById(user.getFamilyId()).orElse(null);

        if (family != null && family.getCreatorId().equals(userId)) {
            List<User> members = userRepository.findByFamilyId(family.getId());
            for (User member : members) {
                member.setFamilyId(null);
                member.setFamilyRole(null);
                userRepository.save(member);
            }
            familyRepository.delete(family);
        } else {
            user.setFamilyId(null);
            user.setFamilyRole(null);
            userRepository.save(user);
        }
    }
}
